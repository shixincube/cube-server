package cube.dispatcher.robot;

import cell.util.log.Logger;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.handler.OpenChannel;
import cube.util.HttpServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONObject;

public class RobotCellet extends AbstractCellet {

    public final static String NAME = "Robot";

    /**
     * 执行机。
     */
    private Performer performer;

    public RobotCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");

        setupHandler();

        registerCallback();

        return true;
    }

    @Override
    public void uninstall() {
        deregisterCallback();
    }

    private void setupHandler() {
        HttpServer httpServer = this.performer.getHttpServer();

        ContextHandler eventHandler = new ContextHandler();
        eventHandler.setContextPath(RoboengineEventHandler.CONTEXT_PATH);
        eventHandler.setHandler(new RoboengineEventHandler(this.performer));
        httpServer.addContextHandler(eventHandler);
    }

    private void registerCallback() {
        HttpClient client = new HttpClient();

        try {
            client.start();

            JSONObject data = new JSONObject();
            data.put("url", Performer.ROBOT_CALLBACK_URL);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(Performer.ROBOT_API_URL).content(provider).send();
            if (response.getStatus() != HttpStatus.OK_200) {
                Logger.w(this.getClass(), "#registerCallback - register callback URL failed: " + response.getStatus());
            }
            else {
                Logger.i(this.getClass(), "#registerCallback - register callback: " + Performer.ROBOT_CALLBACK_URL);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#registerCallback", e);
        } finally {
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                }
            }
        }
    }

    private void deregisterCallback() {
        HttpClient client = new HttpClient();

        try {
            client.start();

            JSONObject data = new JSONObject();
            data.put("url", Performer.ROBOT_CALLBACK_URL);
            data.put("deregister", true);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(Performer.ROBOT_API_URL).content(provider).send();
            if (response.getStatus() != HttpStatus.OK_200) {
                Logger.w(this.getClass(), "#deregisterCallback - deregister callback URL failed: " + response.getStatus());
            }
            else {
                Logger.i(this.getClass(), "#deregisterCallback - deregister callback: " + Performer.ROBOT_CALLBACK_URL);
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#deregisterCallback", e);
        } finally {
            if (client.isStarted()) {
                try {
                    client.stop();
                } catch (Exception e) {
                }
            }
        }
    }
}
