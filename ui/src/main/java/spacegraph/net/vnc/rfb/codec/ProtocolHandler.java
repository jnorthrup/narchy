/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package spacegraph.net.vnc.rfb.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.net.vnc.rfb.codec.decoder.FrameDecoderHandler;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.codec.encoder.*;
import spacegraph.net.vnc.rfb.codec.handshaker.event.ServerInitEvent;
import spacegraph.net.vnc.rfb.exception.ProtocolException;
import spacegraph.net.vnc.rfb.render.ConnectInfoEvent;
import spacegraph.net.vnc.rfb.render.ProtocolConfiguration;
import spacegraph.net.vnc.rfb.render.RenderCallback;
import spacegraph.net.vnc.rfb.render.RenderProtocol;
import spacegraph.net.vnc.rfb.render.rect.ImageRect;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ProtocolHandler extends MessageToMessageDecoder<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);

    private final ProtocolConfiguration config;

    private ServerInitEvent serverInit;

    private final RenderProtocol render;

    private final RenderCallback voidCallback = () -> {
    };

    private final AtomicReference<ProtocolState> state = new AtomicReference<>(ProtocolState.HANDSHAKE_STARTED);

    private SslContext sslContext;

    public ProtocolHandler(RenderProtocol render, ProtocolConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("configuration must not be empty");
        }
        this.config = config;
        this.render = render;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (config.sslProperty().get()) {
            if (sslContext == null) {
                //sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                sslContext = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
            }
            ctx.pipeline().addFirst("ssl-handler", sslContext.newHandler(ctx.channel().alloc()));
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("connection closed");
        if (state.get() == ProtocolState.SECURITY_STARTED) {
            ProtocolException e = new ProtocolException("connection closed without error message");
            render.exceptionCaught(e);
        }
        userEventTriggered(ctx, ProtocolState.CLOSED);
        super.channelInactive(ctx);
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

        if (msg instanceof ImageRect) {
            render.render((ImageRect) msg, voidCallback);
            return;
        }
        if (msg instanceof ServerDecoderEvent) {
            render.eventReceived((ServerDecoderEvent) msg);
            return;
        }

        if (!(msg instanceof ServerInitEvent)) {
            logger.error("unknown message: {}", msg);
            ctx.fireChannelRead(msg);
            return;
        }

        serverInit = (ServerInitEvent) msg;
        logger.debug("handshake completed with {}", serverInit);

        FrameDecoderHandler frameHandler = new FrameDecoderHandler(serverInit.getPixelFormat());
        if (!frameHandler.isPixelFormatSupported()) {
            ProtocolException e = new ProtocolException(String.format("pixelformat: (%s bpp) not supported yet", serverInit.getPixelFormat().getBitPerPixel()));
            exceptionCaught(ctx, e);
            return;
        }

        ChannelPipeline cp = ctx.pipeline();

        cp.addBefore(ctx.name(), "rfb-encoding-encoder", new PreferedEncodingEncoder());
        PreferedEncoding prefEncodings = getPreferedEncodings(FrameDecoderHandler.getSupportedEncodings());
        ctx.write(prefEncodings);

        cp.addBefore(ctx.name(), "rfb-pixelformat-encoder", new PixelFormatEncoder());
        ctx.write(serverInit.getPixelFormat());
        ctx.flush();

        cp.addBefore(ctx.name(), "rfb-frame-handler", frameHandler);
        cp.addBefore(ctx.name(), "rfb-keyevent-encoder", new KeyButtonEventEncoder());
        cp.addBefore(ctx.name(), "rfb-pointerevent-encoder", new PointerEventEncoder());
        cp.addBefore(ctx.name(), "rfb-cuttext-encoder", new ClientCutTextEncoder());

        render.eventReceived(getConnectInfoEvent(ctx, prefEncodings));

        render.registerInputEventListener(event -> ctx.writeAndFlush(event, ctx.voidPromise()));

        logger.debug("request full framebuffer update");
        sendFramebufferUpdateRequest(ctx, false, 0, 0, serverInit.getFrameBufferWidth(), serverInit.getFrameBufferHeight());

        logger.trace("channel pipeline: {}", cp.toMap().keySet());
    }

    private ConnectInfoEvent getConnectInfoEvent(ChannelHandlerContext ctx, PreferedEncoding enc) {
        ConnectInfoEvent details = new ConnectInfoEvent();
        details.setRemoteAddress(ctx.channel().remoteAddress().toString().substring(1));
        details.setServerName(serverInit.getServerName());
        details.setFrameWidth(serverInit.getFrameBufferWidth());
        details.setFrameHeight(serverInit.getFrameBufferHeight());
        details.setRfbProtocol(config.versionProperty().get());
        details.setSecurity(config.securityProperty().get());
        details.setServerPF(serverInit.getPixelFormat());
        details.setClientPF(config.clientPixelFormatProperty().get());
        details.setSupportedEncodings(enc.getEncodings());
        details.setConnectionType(config.sslProperty().get() ? "SSL" : "TCP (standard)");
        return details;
    }

    public PreferedEncoding getPreferedEncodings(Encoding[] supported) {
        Encoding[] enc = Arrays.stream(supported).filter(value -> {
            switch (value) {
                case COPY_RECT:
                    return config.copyRectEncProperty().get();
                case HEXTILE:
                    return config.hextileEncProperty().get();
                case RAW:
                    return config.rawEncProperty().get();
                case CURSOR:
                    return config.clientCursorProperty().get();
                case DESKTOP_SIZE:
                    return config.desktopSizeProperty().get();
                case ZLIB:
                    return config.zlibEncProperty().get();
                default:
                    return true;
            }
        }).toArray(Encoding[]::new);

        logger.info("encodings: {}", Arrays.toString(enc));
        return new PreferedEncoding(enc);
    }

    public static void sendFramebufferUpdateRequest(ChannelHandlerContext ctx, boolean incremental, int x, int y, int w, int h) {
        ByteBuf buf = ctx.alloc().buffer(10, 10);
        buf.writeByte(ClientEventType.FRAMEBUFFER_UPDATE_REQUEST);
        buf.writeByte(incremental ? 1 : 0);

        buf.writeShort(x);
        buf.writeShort(y);
        buf.writeShort(w);
        buf.writeShort(h);

        ctx.writeAndFlush(buf, ctx.voidPromise());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline cp = ctx.pipeline();
        if (cp.get(ProtocolHandshakeHandler.class) == null) {
            cp.addBefore(ctx.name(), "rfb-handshake-handler", new ProtocolHandshakeHandler(config));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage(), cause);
        render.exceptionCaught(cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        logger.trace("user event: {}", evt);
        if (evt instanceof ProtocolState) {
            ProtocolState uvent = (ProtocolState) evt;
            state.set(uvent);
            if (uvent == ProtocolState.FBU_REQUEST) {
                sendFramebufferUpdateRequest(ctx, true, 0, 0, serverInit.getFrameBufferWidth(), serverInit.getFrameBufferHeight());
            }

            render.stateChanged(uvent);
        }
    }
}
