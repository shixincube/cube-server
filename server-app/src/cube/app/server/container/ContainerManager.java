/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.app.server.container;

import cell.util.log.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;

/**
 * 容器管理器。
 */
public class ContainerManager {

    private Server server;

    public ContainerManager() {
    }

    public void launch(int port) {
        this.server = new Server(port);

        this.server.setHandler(createHandlerList());

        Logger.i(ContainerManager.class, "Start cube app server # " + port);

        System.out.println("" +
                "                                             \n" +
                "  ,----..                                    \n" +
                " /   /   \\                ,---,              \n" +
                "|   :     :         ,--,,---.'|              \n" +
                ".   |  ;. /       ,'_ /||   | :              \n" +
                ".   ; /--`   .--. |  | ::   : :      ,---.   \n" +
                ";   | ;    ,'_ /| :  . |:     |,-.  /     \\  \n" +
                "|   : |    |  ' | |  . .|   : '  | /    /  | \n" +
                ".   | '___ |  | ' |  | ||   |  / :.    ' / | \n" +
                "'   ; : .'|:  | : ;  ; |'   : |: |'   ;   /| \n" +
                "'   | '/  :'  :  `--'   \\   | '/ :'   |  / | \n" +
                "|   :    / :  ,      .-./   :    ||   :    | \n" +
                " \\   \\ .'   `--`----'   /    \\  /  \\   \\  /  \n" +
                "  `---`                 `-'----'    `----'   \n" +
                "                                             ");

        System.out.println();

        try {
            this.server.start();
            this.server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HandlerList createHandlerList() {
        HandlerList handlers = new HandlerList();

        handlers.setHandlers(new Handler[] {
            new StopHandler(this.server, this),
            new DefaultHandler()});

        return handlers;
    }

    public void destroy() {

    }
}
