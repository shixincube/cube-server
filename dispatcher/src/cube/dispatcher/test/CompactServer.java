/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
