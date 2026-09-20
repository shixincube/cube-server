#!/usr/bin/env python3
"""
Cube Console 前端本地联调用的 mock 后端。

用途：在没有 MySQL、也不方便启动真实 Java 控制台的环境下，验证前端渲染与交互。

    python3 mock_console_server.py <web_dir> <port>
    # 例如在 console/ 目录下：
    python3 webapp/mock/mock_console_server.py web 8899
    # 然后浏览器打开 http://127.0.0.1:8899/dashboard

它同时承担静态文件服务与 SPA 回退，返回的 JSON 结构严格对齐
`console/src/cube/console/**` 下各 `toJSON()` 的字段。

⚠️ 仅供本地开发/联调使用：
  - 不校验任何凭据，`/signin` 恒定返回管理员令牌；
  - 数据全部是内存里的假数据，不做任何持久化；
  - **绝不能部署到任何可被外部访问的环境**。
"""

import json
import os
import sys
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

WEB_DIR = os.path.abspath(sys.argv[1])
PORT = int(sys.argv[2])

NOW = int(time.time() * 1000)


def ap(host, port, maxconn=64):
    return {"host": host, "port": port, "maxConnection": maxconn}


def endpoint(host, port):
    return {"host": host, "port": port}


def cache_config(host, port, capacity, expiry):
    return {
        "host": host, "port": port, "capacity": capacity, "expiry": expiry,
        "threshold": 10485760, "blocking": 1,
        "storage": "/data/cube/storage", "routetable": "/data/cube/route",
        "clusterNodes": [{"host": "10.0.0.11", "port": 6001}, {"host": "10.0.0.12", "port": 6001}],
        "pedestal": {"host": "10.0.0.11", "port": 6002},
        "backupPedestal": {"host": "10.0.0.12", "port": 6002},
    }


def series_cache_config(host, port):
    return {
        "host": host, "port": port, "capacity": 200000, "segmentNum": 16, "segmentSize": 4096,
        "expiry": 604800000, "indexThreshold": 1048576, "dataThreshold": 4194304, "timeout": 300,
        "storage": "/data/cube/series", "clusterNodes": [{"host": "10.0.0.21", "port": 6101}],
        "pedestal": {"host": "10.0.0.21", "port": 6102},
        "backupPedestal": {"host": "10.0.0.22", "port": 6102},
    }


def dispatcher_server(tag="cube", path="/data/cube/dispatcher", running=True, name_suffix=""):
    return {
        "tag": tag,
        "deployPath": path,
        "name": "%s#dispatcher#%s%s" % (tag, 6800, name_suffix),
        "cellConfigFile": path + "/config/cell.xml",
        "propertiesFile": path + "/config/dispatcher.properties",
        "running": running,
        "server": ap("10.0.0.5", 6800),
        "wsServer": ap("10.0.0.5", 6801),
        "wssServer": ap("10.0.0.5", 6802),
        "http": ap("10.0.0.5", 7080),
        "https": ap("10.0.0.5", 7443),
        "ssl": {"keystore": "config/ssl/keystore.jks",
                "storePassword": "******", "managerPassword": "******"},
        "logLevel": "INFO",
        "cellets": ["cube.dispatcher.core.Performer", "cube.dispatcher.aigc.AIGCCellet"],
        "directors": [
            {"address": "10.0.0.7", "port": 7001, "weight": 60,
             "cellets": ["cube.service.messaging.MessagingCellet", "cube.service.filestorage.FileStorageCellet"]},
            {"address": "10.0.0.8", "port": 7001, "weight": 40,
             "cellets": ["cube.service.contact.ContactCellet"]},
        ],
    }


def service_server(tag="cube", path="/data/cube/service", running=True, name_suffix=""):
    return {
        "tag": tag,
        "deployPath": path,
        "configPath": path + "/config",
        "celletsPath": path + "/cellets",
        "name": "%s#service#%s%s" % (tag, 7001, name_suffix),
        "running": running,
        "server": ap("10.0.0.7", 7001),
        "logLevel": "INFO",
        "cellets": [
            {"ports": [7001], "jar": {"path": path + "/cellets/messaging.jar", "name": "messaging.jar",
                                      "size": 5242880, "lastModified": NOW - 86400000 * 3},
             "classes": ["cube.service.messaging.MessagingCellet"]},
            {"ports": [7001, 7002], "jar": {"path": path + "/cellets/filestorage.jar", "name": "filestorage.jar",
                                            "size": 3145728, "lastModified": NOW - 86400000 * 7},
             "classes": ["cube.service.filestorage.FileStorageCellet"]},
        ],
        "adapter": endpoint("10.0.0.7", 7009),
        "storage": {
            "primary": {"type": "mysql", "host": "10.0.0.30", "port": 3306,
                        "schema": "cube_3", "user": "cube", "password": "******"},
            "secondary": {"type": "sqlite", "file": path + "/storage/secondary.db"},
        },
        "tokenPool": cache_config("10.0.0.11", 6001, 100000, 7200000),
        "generalCache": cache_config("10.0.0.11", 6001, 50000, 3600000),
        "contactCache": cache_config("10.0.0.11", 6001, 80000, 7200000),
        "fileLabelCache": cache_config("10.0.0.12", 6001, 20000, 1800000),
        "messagingSeries": series_cache_config("10.0.0.21", 6101),
    }


DISPATCHERS = [
    dispatcher_server("cube", "/data/cube/dispatcher", True),
    dispatcher_server("cube", "/data/cube/dispatcher-bak", False, "-bak"),
]

SERVICES = [
    service_server("cube", "/data/cube/service", True),
    service_server("cube", "/data/cube/service-bak", False, "-bak"),
]

USER = {"name": "admin", "avatar": "/assets/img/avatar.png",
        "displayName": "控制台管理员", "role": 1, "group": "运营"}


def statistic():
    slices = []
    base = NOW - 86400000
    for i in range(12):
        slices.append({
            "beginning": base + i * 7200000,
            "ending": base + (i + 1) * 7200000,
            "numContacts": 120 + (i * 37) % 260,
        })
    return {"TNU": 18432, "DAU": 3175, "AOT": 642, "TD": slices, "DNU": 218}


def jvm_reports(count=30):
    out = []
    for i in range(count):
        total = 4 * 1024 * 1024 * 1024
        free = int(total * (0.35 + 0.25 * ((i * 7) % 10) / 10.0))
        out.append({
            "name": "JVMReport", "timestamp": NOW - (count - i) * 10000,
            "reporter": DISPATCHERS[0]["name"],
            "maxMemory": 6 * 1024 * 1024 * 1024, "totalMemory": total, "freeMemory": free,
            "systemStartTime": NOW - 86400000 * 5, "systemDuration": 86400000 * 5,
        })
    return out


def performance_report():
    return {
        "name": "PerfReport", "timestamp": NOW - 5000, "reporter": DISPATCHERS[0]["name"],
        "systemStartTime": NOW - 86400000 * 5, "systemDuration": 86400000 * 5,
        "connNums": [
            {"port": 6800, "realtime": 128, "max": 512},
            {"port": 6801, "realtime": 342, "max": 1024},
            {"port": 6802, "realtime": 57, "max": 256},
        ],
        "items": {
            "Messaging": {"onlineNum": 2210, "maxNum": 5000},
            "FileStorage": {"onlineNum": 1480, "maxNum": 3000},
            "Contact": {"onlineNum": 964, "maxNum": 2000},
        },
        "benchmark": {
            "avgResponseTimeMap": {
                "Contact": {
                    "Contact.list": {"value": 8.2, "delta": 0.4},
                    "Contact.get": {"value": 6.7, "delta": -0.3},
                },
                "Messaging": {
                    "Messaging.send": {"value": 12.4, "delta": -1.8},
                    "Messaging.broadcast": {"value": 26.9, "delta": 1.1},
                },
                "FileStorage": {
                    "FileStorage.upload": {"value": 86.7, "delta": 4.2},
                    "FileStorage.download": {"value": 214.5, "delta": -6.1},
                },
            },
            "counterMap": {"Contact": 92330, "FileStorage": 47210, "Messaging": 184320},
        },
    }


def logs(name, start):
    lines = []
    level = [2, 2, 2, 3, 2, 4, 2, 1]
    texts = [
        "Performer - director route table updated, nodes=2",
        "Block - waiting device response, token=9f2c",
        "Transmission - async send completed, size=2048",
        "ConnectionPool - borrowed connection, active=7",
        "AIGCCellet - task queued, action=GenerateText",
        "ConnectionPool - reconnect attempt 1 of 3",
        "ServiceCarpet - heartbeat ok, daemon reported",
        "Cache - token pool hit ratio 0.94",
    ]
    for i in range(24):
        lines.append({
            "time": NOW - (24 - i) * 1500,
            "level": level[i % len(level)],
            "tag": "cube.dispatcher" if i % 2 == 0 else "cube.service",
            "text": texts[i % len(texts)],
        })
    return {"name": name, "lines": lines, "last": start + len(lines)}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        pass

    # ---------- helpers ----------

    def send_json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=UTF-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_form(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length).decode("utf-8") if length else ""
        return {k: v[0] for k, v in urllib.parse.parse_qs(raw).items()}

    def query(self):
        q = urllib.parse.urlparse(self.path).query
        return {k: v[0] for k, v in urllib.parse.parse_qs(q).items()}

    def serve_static(self, path):
        rel = path.lstrip("/") or "index.html"
        full = os.path.join(WEB_DIR, rel)
        if not os.path.isfile(full):
            return False
        ext = os.path.splitext(full)[1].lower()
        ctype = {
            ".html": "text/html; charset=UTF-8", ".js": "application/javascript",
            ".css": "text/css", ".png": "image/png", ".jpg": "image/jpeg",
            ".svg": "image/svg+xml", ".ico": "image/x-icon", ".json": "application/json",
        }.get(ext, "application/octet-stream")
        with open(full, "rb") as f:
            body = f.read()
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
        return True

    def spa_index(self):
        with open(os.path.join(WEB_DIR, "index.html"), "rb") as f:
            body = f.read()
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=UTF-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    # ---------- routes ----------

    def do_GET(self):
        path = urllib.parse.urlparse(self.path).path
        q = self.query()

        if path == "/servers/dispatcher":
            return self.send_json({"tag": "cube", "list": DISPATCHERS})
        if path == "/servers/service":
            return self.send_json({"tag": "cube", "list": SERVICES})
        if path == "/deploy/dispatcher":
            return self.send_json({"tag": "cube", "deployPath": "/data/cube/dispatcher",
                                   "cellConfigFile": "/data/cube/dispatcher/config/cell.xml",
                                   "propertiesFile": "/data/cube/dispatcher/config/dispatcher.properties"})
        if path == "/deploy/service":
            return self.send_json({"tag": "cube", "deployPath": "/data/cube/service",
                                   "configPath": "/data/cube/service/config",
                                   "celletsPath": "/data/cube/service/cellets"})
        if path == "/auth/domain":
            return self.send_json({"tag": "cube", "list": ["shixincube.com", "demo.shixincube.com"]})
        if path in ("/statistic/recent", "/statistic/daily"):
            return self.send_json({"tag": "cube", "statistic": statistic(),
                                   "year": 2026, "month": 9, "date": 19})
        if path == "/log/console":
            return self.send_json(logs("console", int(q.get("start", 0))))
        if path == "/log/server":
            return self.send_json(logs(q.get("name", "server"), int(q.get("start", 0))))
        if path == "/server-report":
            report = q.get("report", "JVMReport")
            if report == "JVMReport":
                return self.send_json({"name": "JVMReport", "list": jvm_reports()})
            return self.send_json({"name": "PerfReport", "report": performance_report()})

        # 静态资源 / SPA 回退
        if self.serve_static(path):
            return
        if path == "/" or "." not in os.path.basename(path):
            return self.spa_index()
        self.send_json({"error": "not found"}, 404)

    def do_POST(self):
        path = urllib.parse.urlparse(self.path).path

        if path == "/signin":
            return self.send_json({"token": "mock-token", "creation": NOW,
                                   "expire": NOW + 604800000, "user": USER})
        if path == "/signout":
            return self.send_json({})
        if path in ("/dispatcher/status", "/dispatcher/start", "/dispatcher/stop",
                    "/dispatcher/config"):
            return self.send_json(DISPATCHERS[0])
        if path in ("/service/status", "/service/start", "/service/stop", "/service/config"):
            return self.send_json(SERVICES[0])
        if path == "/report":
            return self.send_json({})
        self.send_json({"error": "not found"}, 404)


if __name__ == "__main__":
    print("mock console server on http://127.0.0.1:%d  web=%s" % (PORT, WEB_DIR), flush=True)
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
