package nars.op.language.util;

import com.google.common.collect.ImmutableSortedSet;
import org.eclipse.collections.impl.factory.Iterables;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.InputParser;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.OutputEvent;
import org.pircbotx.output.OutputIRC;

import java.io.IOException;
import java.util.List;

/**
 * Generic IRC Bot interface via PircBotX
 */
public class IRC extends ListenerAdapter {








    public final PircBotX irc;








    public IRC(String nick, String server, String... channels) {
        this(new Configuration.Builder()
                .setName(nick)
                .setRealName(nick)
                .setLogin("root")
                .setVersion("unknown")
                .addServer(server)
                .addAutoJoinChannels(Iterables.mList(channels))
                .setAutoReconnect(true)
                .setAutoNickChange(true)
        );

    }

    public IRC(Configuration.Builder cb)  {

        cb.addListener(this);

        this.irc = new PircBotX(cb.buildConfiguration()) {
            @Override
            protected void sendRawLineToServer(String line) throws IOException {
                if (line.length() > configuration.getMaxLineLength() - 2) line = line.substring(0, configuration.getMaxLineLength() - 2);
                outputWriter.write(line + "\r\n");
                outputWriter.flush();
                List<String> lineParts = InputParser.tokenizeLine(line);
                getConfiguration().getListenerManager().onEvent(new OutputEvent(this, line, lineParts));
            }

        };

    }

    public final IRC start() throws IOException, IrcException {
        irc.startBot();
        return this;
    }

    public final void stop() {
        irc.stopBotReconnect();
    }


    public void broadcast(String message) {
        if (irc.isConnected()) {
            ImmutableSortedSet<Channel> chans = irc.getUserChannelDao().getAllChannels();
            for (Channel c : chans) {
                irc.send().message(c.getName(), message);
            }

        }
    }

    public Runnable send(String[] channels, String message) {
        if (irc.isConnected()) {
            OutputIRC out = irc.send();
            return ()-> {
                for (String c : channels)
                    out.message(c, message);
            };
        } else {
            return null;
        }
    }








}
