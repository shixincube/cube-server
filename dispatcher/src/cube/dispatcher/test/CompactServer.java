/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher.test;

import cell.api.CelletService;
import cell.api.Nucleus;
import cell.api.TalkService;
import cube.dispatcher.contact.ContactCellet;
import cube.dispatcher.messaging.MessagingCellet;

import java.util.Scanner;

public class CompactServer {

    public CompactServer() {

    }

    public static void main(String[] args) {
        Nucleus nucleus = new Nucleus();

        // Talk 服务器启动
        TalkService talkService = nucleus.getTalkService();
        if (null == talkService.startServer(7000)) {
            System.out.println("Start server failed");
            return;
        }

        if (null == talkService.startWebSocketServer(7070)) {
            System.out.println("Start WebSocket server failed");
            return;
        }

        ContactCellet contactCellet = new ContactCellet();
        MessagingCellet messagingCellet = new MessagingCellet();

        CelletService celletService = nucleus.getCelletService();
        // 安装 Cellet
        celletService.installCellet(contactCellet);
        celletService.installCellet(messagingCellet);

        // 激活 Cellet
        if (!celletService.activateCellet(7000, contactCellet) ||
            !celletService.activateCellet(7000, messagingCellet)) {
            System.out.println("Activate cellets (7000) failed");
            talkService.stopAllServers();
            nucleus.destroy();
            return;
        }
        if (!celletService.activateCellet(7070, contactCellet) ||
            !celletService.activateCellet(7070, messagingCellet)) {
            System.out.println("Activate cellets (7070) failed");
            talkService.stopAllServers();
            nucleus.destroy();
            return;
        }

        System.out.println("*** Input 'q' to exit.");
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            if (sc.next().toLowerCase().equals("q")) {
                break;
            }
        }
        sc.close();

        talkService.stopAllServers();

        celletService.deactivateCellet(7000, contactCellet);
        celletService.deactivateCellet(7000, messagingCellet);
        celletService.deactivateCellet(7070, contactCellet);
        celletService.deactivateCellet(7070, messagingCellet);

        celletService.uninstallCellet(contactCellet);
        celletService.uninstallCellet(messagingCellet);

        nucleus.destroy();

        System.out.println("!!! Server stop.");
    }
}
