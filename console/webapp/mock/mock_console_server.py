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
import math
import os
import sys
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

WEB_DIR = os.path.abspath(sys.argv[1])
PORT = int(sys.argv[2])

NOW = int(time.time() * 1000)

KB = 1024
MB = 1024 * KB
GB = 1024 * MB

HOST_TOTAL_MEMORY = 32 * GB


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


def dispatcher_server(tag="cube", path="/data/cube/dispatcher", running=True, name_suffix="",
                      version="3.0.157"):
    return {
        "tag": tag,
        "deployPath": path,
        "name": "%s#dispatcher#%s%s" % (tag, 6800, name_suffix),
        "version": version,
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


def service_server(tag="cube", path="/data/cube/service", running=True, name_suffix="",
                   version="3.0.250"):
    return {
        "tag": tag,
        "deployPath": path,
        "configPath": path + "/config",
        "celletsPath": path + "/cellets",
        "name": "%s#service#%s%s" % (tag, 7001, name_suffix),
        "version": version,
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
    # `-bak` 一行刻意给空版本号，用于验证「读不到版本时显示 --」
    dispatcher_server("cube", "/data/cube/dispatcher-bak", False, "-bak", ""),
]

SERVICES = [
    service_server("cube", "/data/cube/service", True),
    service_server("cube", "/data/cube/service-bak", False, "-bak", ""),
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


def capability(name, task, subtasks, description, version="1.0"):
    """AICapability 的线格式：单个子任务时后端给 `subtask`，多个子任务时给 `subtasks`。"""
    item = {"name": name, "task": task, "version": version, "description": description}
    if len(subtasks) == 1:
        item["subtask"] = subtasks[0]
    else:
        item["subtasks"] = subtasks
    return item


# 台账的每一行是一个 Contact 物理实体：同一个实体上可以注册多个 AIGC 单元，
# 因此 `capabilities` 是一个数组（覆盖多个能力 / 单个能力 / 未上报能力三种情况）。
# 单元名刻意不与 ID 同序，否则按名称排序看不出效果。
UNIT_ROWS = [
    (100001, "Unit-531001", "AI-Workstation-01", "Linux 6.1", "online", 128, 6.42,
     [
         capability("TextGeneration", "NaturalLanguageProcessing", ["TextGeneration"], "文本生成单元"),
         capability("TextToImage", "Multimodal", ["TextToImage"], "文生图单元"),
         capability("SentimentAnalysis", "NaturalLanguageProcessing", ["SentimentAnalysis"], "情感分析单元"),
     ]),
    (100002, "Unit-105887", "AI-Workstation-02", "Linux 6.1", "online", 96, 5.18,
     [capability("Segmentation", "NaturalLanguageProcessing", ["Segmentation"], "分词单元")]),
    (100003, "Unit-914070", "Edge-Box-11", "Ubuntu 22.04", "online", 41, 3.05,
     [capability("AutomaticSpeechRecognition", "AudioProcessing",
                 ["AutomaticSpeechRecognition"], "语音识别单元"),
      capability("TextToSpeech", "AudioProcessing", ["TextToSpeech"], "语音合成单元")]),
    (100004, "Unit-362104", "Edge-Box-12", "Ubuntu 22.04", "offline", 12, 0.86,
     [capability("Summarization", "NaturalLanguageProcessing", ["Summarization"], "摘要单元")]),
    (100005, "Unit-778203", "GPU-Server-A", "Linux 5.15", "offline", 7, 0.44,
     [capability("MultimodalQA", "Multimodal",
                 ["VisualQuestionAnswering", "DocumentQuestionAnswering"], "多模态问答单元")]),
    (100006, "Unit-406612", "GPU-Server-B", "Linux 5.15", "inactive", 0, 0.0, []),
    (100007, "Unit-620455", "nas-node-7", "Linux 6.1", "inactive", 0, 0.0,
     [capability("ExtractURLContent", "DataProcessing", ["ExtractURLContent"], "网页正文抽取单元")]),
]


def unit_overview():
    """`GET /statistic/units` 的响应。

    控制台的 `UnitReport` 上报通道在这里不参与，能力直接写死在 mock 里，
    用于离线预览「能力」列、状态筛选与名称排序。
    """
    day = NOW - 86400000
    units = []
    online = offline = inactive = 0
    active = 0
    total_duration = 0.0

    for (uid, name, device, platform, state, active_count, duration, caps) in UNIT_ROWS:
        if state == "online":
            online += 1
            last_active = NOW - (uid % 40) * 60000
        elif state == "offline":
            offline += 1
            last_active = NOW - 86400000 - (uid % 12) * 3600000
        else:
            inactive += 1
            last_active = 0

        if active_count > 0:
            active += 1
            total_duration += duration

        units.append({
            "id": uid,
            "name": name,
            "device": device,
            "platform": platform,
            "state": state,
            "activeCount": active_count,
            "duration": duration,
            "lastActiveTime": last_active,
            "firstActiveTime": (day + 9 * 3600000) if active_count > 0 else 0,
            "activeDays": min(30, (uid % 27) + 1),
            "capabilities": caps,
        })

    timeline = []
    peak = None
    for i in range(24):
        num = 0 if i < 7 else int(1 + 3.5 * math.sin(math.pi * (i - 7) / 16.0))
        num = max(0, num)
        slice_item = {"slice": i, "beginning": day + i * 3600000,
                      "ending": day + (i + 1) * 3600000, "numUnits": num}
        timeline.append(slice_item)
        if num > 0 and (peak is None or num > peak["numUnits"]):
            peak = slice_item

    return {
        "tag": "cube",
        "domain": "shixincube.com",
        "year": 2026, "month": 9, "date": 20,
        "beginning": day, "ending": day + 86400000,
        "total": len(units),
        "activeCount": active,
        "onlineCount": online,
        "offlineCount": offline,
        "inactiveCount": inactive,
        "totalDuration": round(total_duration, 2),
        "avgDuration": round(total_duration / active, 2) if active > 0 else 0.0,
        "peak": peak,
        "timeline": timeline,
        "units": units,
        "unitReportTime": NOW - 18000,
    }


def jvm_reports(count=30):
    """注意单位：控制台在 `Console#appendJVMReport` 里已把上报字节数换算成 **MB** 后入库，
    前端 `toJVMSeries` 因此不再做除法 —— mock 必须同样返回 MB，否则纵轴会大三个数量级。"""
    out = []
    for i in range(count):
        total = 4096
        free = int(total * (0.35 + 0.25 * ((i * 7) % 10) / 10.0))
        out.append({
            "name": "JVMReport", "timestamp": NOW - (count - i) * 10000,
            "reporter": DISPATCHERS[0]["name"],
            "maxMemory": 6144, "totalMemory": total, "freeMemory": free,
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


BOOT_MS = NOW - 86400000 * 6


def _wave(period, phase=0.0, low=0.0, high=1.0):
    """在 low~high 之间按周期平滑摆动：让前端的实时曲线真的在动"""
    ratio = 0.5 + 0.5 * math.sin(2 * math.pi * time.time() / period + phase)
    return low + (high - low) * ratio


def _accumulated(bytes_per_sec_avg, offset):
    """按开机时长推算累计流量，保证单调递增"""
    return int((time.time() * 1000 - BOOT_MS) / 1000.0 * bytes_per_sec_avg) + offset


def host_static():
    return {
        "sampledAt": NOW,
        "host": {
            "hostName": "cube-console-mock", "osName": "macOS", "osVersion": "14.5",
            "osArch": "aarch64", "timezone": "Asia/Shanghai", "osFamily": "macOS",
            "osManufacturer": "Apple Inc.", "bitness": 64, "bootTime": BOOT_MS,
            "osVersionInfo": "14.5.0", "osBuildNumber": "23F79", "osCodeName": "Sonoma",
        },
        "system": {
            "manufacturer": "Apple Inc.", "model": "MacBook Pro (M2 Pro, 2023)",
            "serialNumber": "MOCK0000001", "uuid": "00000000-0000-0000-0000-000000000001",
            "firmware": {"manufacturer": "Apple Inc.", "name": "iBoot",
                         "version": "10151.121.1", "releaseDate": "2024-05-01"},
            "baseboard": {"manufacturer": "Apple Inc.", "model": "Mac14,10",
                          "version": "1.0", "serialNumber": "MOCKBOARD0001"},
        },
        "cpu": {
            "name": "Apple M2 Pro", "vendor": "Apple", "family": "ARM", "model": "0",
            "stepping": "0", "microarchitecture": "ARMv8.6", "processorId": "0x0",
            "identifier": "Apple M2 Pro", "cpu64bit": True,
            "physicalCores": 12, "logicalCores": 12, "physicalPackages": 1,
            "maxFrequencyHz": 3500000000,
            "caches": [
                {"level": 1, "type": "Data", "sizeBytes": 128 * KB},
                {"level": 2, "type": "Unified", "sizeBytes": 16 * MB},
            ],
        },
        "memory": {
            "totalBytes": HOST_TOTAL_MEMORY, "pageSizeBytes": 16384,
            "modules": [
                {"bankLabel": "A1", "capacityBytes": 16 * GB, "clockSpeedHz": 6400000000,
                 "manufacturer": "Micron", "memoryType": "LPDDR5", "partNumber": "MT53E2G32D4NQ-046"},
                {"bankLabel": "A2", "capacityBytes": 16 * GB, "clockSpeedHz": 6400000000,
                 "manufacturer": "Micron", "memoryType": "LPDDR5", "partNumber": "MT53E2G32D4NQ-046"},
            ],
        },
        "diskDrives": [
            {"name": "disk0", "model": "APPLE SSD AP1024Z", "serial": "MOCKSSD0001",
             "sizeBytes": 1000 * GB,
             "partitions": [{"identification": "disk1s1", "name": "Macintosh HD", "type": "APFS",
                             "label": "Macintosh HD", "sizeBytes": 994 * GB, "mountPoint": "/"}]},
        ],
        "nicList": [
            {"name": "en0", "displayName": "Wi-Fi (AirPort)", "up": True, "mtu": 1500,
             "mac": "a0:00:00:00:00:01", "addresses": ["192.168.1.20", "fe80::10:20:30:40"]},
            {"name": "en5", "displayName": "Ethernet Adapter (en5)", "up": True, "mtu": 1500,
             "mac": "a0:00:00:00:00:02", "addresses": ["10.0.0.5"]},
            {"name": "utun0", "displayName": "utun0", "up": False, "mtu": 1380,
             "mac": "", "addresses": []},
        ],
        "runtime": {
            "javaVersion": "1.8.0_131", "javaVendor": "Oracle Corporation",
            "jvmName": "OpenJDK 64-Bit Server VM", "jvmVersion": "25.131-b11",
            "availableProcessors": 12, "configuredMaxHeapBytes": 2 * GB,
            "fileEncoding": "UTF-8", "userName": "cube", "workDir": "/data/cube/console",
            "pid": 4321, "startTime": NOW - 86400000 * 2,
        },
    }


def host_metrics():
    cpu_usage = _wave(37, 0.0, 0.04, 0.63)
    idle = 1.0 - cpu_usage
    mem_usage = _wave(180, 1.2, 0.42, 0.72)
    used_mem = int(HOST_TOTAL_MEMORY * mem_usage)
    rx = _wave(23, 2.0, 180_000, 5_600_000)
    tx = _wave(19, 0.7, 90_000, 1_900_000)
    read_rate = _wave(29, 1.6, 0, 42_000_000)
    write_rate = _wave(31, 2.4, 0, 18_000_000)
    heap_max = 2 * GB
    heap_used = int(heap_max * _wave(97, 0.5, 0.28, 0.66))
    host_threads = int(_wave(59, 0.4, 2100, 2680))
    jvm_threads = int(_wave(41, 1.1, 88, 164))
    swap_total = 4 * GB
    swap_used = int(swap_total * _wave(211, 0.9, 0.02, 0.18))

    def partition(name, mount, total, usage_wave):
        total_bytes = int(total)
        usage = usage_wave
        free = int(total_bytes * (1.0 - usage))
        return {"name": name, "mount": mount, "type": "APFS", "local": True,
                "totalBytes": total_bytes, "usableBytes": free + 12 * GB,
                "freeBytes": free, "usage": round(usage, 4)}

    return {
        "timestamp": int(time.time() * 1000),
        "sampleIntervalSeconds": 5,
        "cpu": {
            "usage": round(cpu_usage, 4),
            "breakdown": {
                "user": round(cpu_usage * 0.62, 4),
                "system": round(cpu_usage * 0.26, 4),
                "iowait": round(cpu_usage * 0.05, 4),
                "irq": round(cpu_usage * 0.03, 4),
                "nice": round(cpu_usage * 0.02, 4),
                "steal": round(cpu_usage * 0.02, 4),
                "idle": round(idle, 4),
            },
            "loadAverage": [round(cpu_usage * 12 * 0.9, 2), round(cpu_usage * 12 * 0.7, 2),
                            round(cpu_usage * 12 * 0.5, 2)],
            "contextSwitches": int(_wave(53, 0.3, 1_200_000, 3_800_000)),
            "interrupts": int(_wave(47, 1.7, 400_000, 1_100_000)),
            "logicalCores": 12,
            "processUsage": round(cpu_usage * 0.12, 4),
        },
        "memory": {
            "totalBytes": HOST_TOTAL_MEMORY,
            "availableBytes": HOST_TOTAL_MEMORY - used_mem,
            "usedBytes": used_mem,
            "usage": round(mem_usage, 4),
            "swapTotalBytes": swap_total,
            "swapUsedBytes": swap_used,
            "swapUsage": round(swap_used / swap_total, 4),
        },
        "jvm": {
            "heapUsedBytes": heap_used,
            "heapCommittedBytes": int(heap_max * 0.75),
            "heapMaxBytes": heap_max,
            "heapUsage": round(heap_used / heap_max, 4),
            "nonHeapUsedBytes": int(_wave(73, 0.2, 42 * MB, 96 * MB)),
            "threadCount": jvm_threads,
            "peakThreadCount": jvm_threads + 24,
            "uptimeMillis": 86400000 * 2,
            "startTime": NOW - 86400000 * 2,
        },
        "network": {
            "rxBytesPerSec": int(rx),
            "txBytesPerSec": int(tx),
            "rxBytesTotal": _accumulated(320_000, 42 * GB),
            "txBytesTotal": _accumulated(120_000, 9 * GB),
            "rxPacketsTotal": _accumulated(640, 88_000_000),
            "txPacketsTotal": _accumulated(520, 61_000_000),
            "activeInterfaces": [
                {"name": "en0", "displayName": "Wi-Fi (AirPort)", "mac": "a0:00:00:00:00:01",
                 "ipv4": ["192.168.1.20"], "mtu": 1500, "linkSpeedBps": 866_000_000,
                 "rxBytesTotal": _accumulated(300_000, 40 * GB),
                 "txBytesTotal": _accumulated(110_000, 8 * GB),
                 "rxBytesPerSec": int(rx * 0.8), "txBytesPerSec": int(tx * 0.85),
                 "inErrors": 0, "outErrors": 0},
                {"name": "en5", "displayName": "Ethernet Adapter (en5)", "mac": "a0:00:00:00:00:02",
                 "ipv4": ["10.0.0.5"], "mtu": 1500, "linkSpeedBps": 1_000_000_000,
                 "rxBytesTotal": _accumulated(20_000, 2 * GB),
                 "txBytesTotal": _accumulated(10_000, 1 * GB),
                 "rxBytesPerSec": int(rx * 0.2), "txBytesPerSec": int(tx * 0.15),
                 "inErrors": 0, "outErrors": 3},
            ],
        },
        "disk": {
            "readBytesPerSec": int(read_rate),
            "writeBytesPerSec": int(write_rate),
            "readBytesTotal": _accumulated(1_400_000, 180 * GB),
            "writeBytesTotal": _accumulated(900_000, 96 * GB),
            "drives": [
                {"name": "disk0", "model": "APPLE SSD AP1024Z", "sizeBytes": 1000 * GB,
                 "readBytesTotal": _accumulated(1_300_000, 170 * GB),
                 "writeBytesTotal": _accumulated(850_000, 92 * GB),
                 "readBytesPerSec": int(read_rate * 0.92),
                 "writeBytesPerSec": int(write_rate * 0.95),
                 "readsTotal": _accumulated(900, 120_000_000),
                 "writesTotal": _accumulated(700, 96_000_000), "queueLength": 0},
            ],
            "partitions": [
                partition("disk1s1", "/", 994 * GB, _wave(400, 0.4, 0.46, 0.52)),
                partition("disk1s4", "/System/Volumes/Data", 994 * GB, _wave(380, 1.1, 0.44, 0.50)),
                partition("disk2s1", "/Volumes/Backup", 2000 * GB, _wave(360, 2.2, 0.18, 0.24)),
            ],
        },
        "system": {
            "processCount": int(_wave(67, 1.4, 480, 620)),
            "threadCount": host_threads,
            "uptimeSeconds": int((time.time() * 1000 - BOOT_MS) / 1000),
            "bootTime": BOOT_MS,
            "openFileDescriptors": int(_wave(71, 0.8, 1800, 2600)),
            "maxFileDescriptors": 10240,
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
        if path == "/statistic/units":
            return self.send_json(unit_overview())
        if path == "/log/console":
            return self.send_json(logs("console", int(q.get("start", 0))))
        if path == "/log/server":
            return self.send_json(logs(q.get("name", "server"), int(q.get("start", 0))))
        if path == "/host/static":
            return self.send_json(host_static())
        if path == "/host/metrics":
            return self.send_json(host_metrics())
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
