package nars;

import jcog.Texts;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.net.http.HttpConnection;
import jcog.net.http.HttpModel;
import jcog.net.http.HttpServer;
import nars.index.concept.MaplikeConceptIndex;
import nars.index.concept.ProxyConceptIndex;
import nars.web.WebClientJS;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.teavm.tooling.RuntimeCopyOperation;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.util.function.Consumer;
import java.util.function.Function;

import static jcog.data.map.CustomConcurrentHashMap.*;

public class Web implements HttpModel {

    static final int DEFAULT_PORT = 60606;


    private final NAR nar;
    private final MaplikeConceptIndex sharedIndexAdapter;

    @Override
    public void response(HttpConnection h) {

        URI url = h.url();


            String path = url.getPath();
            switch (path) {
                case "/teavm/runtime.js":
                    h.respond(new File("/tmp/tea/runtime.js"));
                    break;
                case "/teavm/classes.js":
                    h.respond(new File("/tmp/tea/classes.js"));
                    break;
                case "/websocket.js":
                    h.respond("// Copyright: Hiroshi Ichikawa <http://gimite.net/en/>\n" +
                            "// License: New BSD License\n" +
                            "// Reference: http://dev.w3.org/html5/websockets/\n" +
                            "// Reference: http://tools.ietf.org/html/rfc6455\n" +
                            "\n" +
                            "(function() {\n" +
                            "  \n" +
                            "  if (window.WEB_SOCKET_FORCE_FLASH) {\n" +
                            "    // Keeps going.\n" +
                            "  } else if (window.WebSocket) {\n" +
                            "    return;\n" +
                            "  } else if (window.MozWebSocket) {\n" +
                            "    // Firefox.\n" +
                            "    window.WebSocket = MozWebSocket;\n" +
                            "    return;\n" +
                            "  }\n" +
                            "  \n" +
                            "  var logger;\n" +
                            "  if (window.WEB_SOCKET_LOGGER) {\n" +
                            "    logger = WEB_SOCKET_LOGGER;\n" +
                            "  } else if (window.console && window.console.log && window.console.error) {\n" +
                            "    // In some environment, console is defined but console.log or console.error is missing.\n" +
                            "    logger = window.console;\n" +
                            "  } else {\n" +
                            "    logger = {log: function(){ }, error: function(){ }};\n" +
                            "  }\n" +
                            "  \n" +
                            "  // swfobject.hasFlashPlayerVersion(\"10.0.0\") doesn't work with Gnash.\n" +
                            "  if (swfobject.getFlashPlayerVersion().major < 10) {\n" +
                            "    logger.error(\"Flash Player >= 10.0.0 is required.\");\n" +
                            "    return;\n" +
                            "  }\n" +
                            "  if (location.protocol == \"file:\") {\n" +
                            "    logger.error(\n" +
                            "      \"WARNING: web-socket-js doesn't work in file:///... URL \" +\n" +
                            "      \"unless you set Flash Security Settings properly. \" +\n" +
                            "      \"Open the page via Web server i.e. http://...\");\n" +
                            "  }\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Our own implementation of WebSocket class using Flash.\n" +
                            "   * @param {string} url\n" +
                            "   * @param {array or string} protocols\n" +
                            "   * @param {string} proxyHost\n" +
                            "   * @param {int} proxyPort\n" +
                            "   * @param {string} headers\n" +
                            "   */\n" +
                            "  window.WebSocket = function(url, protocols, proxyHost, proxyPort, headers) {\n" +
                            "    var self = this;\n" +
                            "    self.__id = WebSocket.__nextId++;\n" +
                            "    WebSocket.__instances[self.__id] = self;\n" +
                            "    self.readyState = WebSocket.CONNECTING;\n" +
                            "    self.bufferedAmount = 0;\n" +
                            "    self.__events = {};\n" +
                            "    if (!protocols) {\n" +
                            "      protocols = [];\n" +
                            "    } else if (typeof protocols == \"string\") {\n" +
                            "      protocols = [protocols];\n" +
                            "    }\n" +
                            "    // Uses setTimeout() to make sure __createFlash() runs after the caller sets ws.onopen etc.\n" +
                            "    // Otherwise, when onopen fires immediately, onopen is called before it is set.\n" +
                            "    self.__createTask = setTimeout(function() {\n" +
                            "      WebSocket.__addTask(function() {\n" +
                            "        self.__createTask = null;\n" +
                            "        WebSocket.__flash.create(\n" +
                            "            self.__id, url, protocols, proxyHost || null, proxyPort || 0, headers || null);\n" +
                            "      });\n" +
                            "    }, 0);\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Send data to the web socket.\n" +
                            "   * @param {string} data  The data to send to the socket.\n" +
                            "   * @return {boolean}  True for success, false for failure.\n" +
                            "   */\n" +
                            "  WebSocket.prototype.send = function(data) {\n" +
                            "    if (this.readyState == WebSocket.CONNECTING) {\n" +
                            "      throw \"INVALID_STATE_ERR: Web Socket connection has not been established\";\n" +
                            "    }\n" +
                            "    // We use encodeURIComponent() here, because FABridge doesn't work if\n" +
                            "    // the argument includes some characters. We don't use escape() here\n" +
                            "    // because of this:\n" +
                            "    // https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Functions#escape_and_unescape_Functions\n" +
                            "    // But it looks decodeURIComponent(encodeURIComponent(s)) doesn't\n" +
                            "    // preserve all Unicode characters either e.g. \"\\uffff\" in Firefox.\n" +
                            "    // Note by wtritch: Hopefully this will not be necessary using ExternalInterface.  Will require\n" +
                            "    // additional testing.\n" +
                            "    var result = WebSocket.__flash.send(this.__id, encodeURIComponent(data));\n" +
                            "    if (result < 0) { // success\n" +
                            "      return true;\n" +
                            "    } else {\n" +
                            "      this.bufferedAmount += result;\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Close this web socket gracefully.\n" +
                            "   */\n" +
                            "  WebSocket.prototype.close = function() {\n" +
                            "    if (this.__createTask) {\n" +
                            "      clearTimeout(this.__createTask);\n" +
                            "      this.__createTask = null;\n" +
                            "      this.readyState = WebSocket.CLOSED;\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    if (this.readyState == WebSocket.CLOSED || this.readyState == WebSocket.CLOSING) {\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    this.readyState = WebSocket.CLOSING;\n" +
                            "    WebSocket.__flash.close(this.__id);\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Implementation of {@link <a href=\"http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-registration\">DOM 2 EventTarget Interface</a>}\n" +
                            "   *\n" +
                            "   * @param {string} type\n" +
                            "   * @param {function} listener\n" +
                            "   * @param {boolean} useCapture\n" +
                            "   * @return void\n" +
                            "   */\n" +
                            "  WebSocket.prototype.addEventListener = function(type, listener, useCapture) {\n" +
                            "    if (!(type in this.__events)) {\n" +
                            "      this.__events[type] = [];\n" +
                            "    }\n" +
                            "    this.__events[type].push(listener);\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Implementation of {@link <a href=\"http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-registration\">DOM 2 EventTarget Interface</a>}\n" +
                            "   *\n" +
                            "   * @param {string} type\n" +
                            "   * @param {function} listener\n" +
                            "   * @param {boolean} useCapture\n" +
                            "   * @return void\n" +
                            "   */\n" +
                            "  WebSocket.prototype.removeEventListener = function(type, listener, useCapture) {\n" +
                            "    if (!(type in this.__events)) return;\n" +
                            "    var events = this.__events[type];\n" +
                            "    for (var i = events.length - 1; i >= 0; --i) {\n" +
                            "      if (events[i] === listener) {\n" +
                            "        events.splice(i, 1);\n" +
                            "        break;\n" +
                            "      }\n" +
                            "    }\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Implementation of {@link <a href=\"http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-registration\">DOM 2 EventTarget Interface</a>}\n" +
                            "   *\n" +
                            "   * @param {Event} event\n" +
                            "   * @return void\n" +
                            "   */\n" +
                            "  WebSocket.prototype.dispatchEvent = function(event) {\n" +
                            "    var events = this.__events[event.type] || [];\n" +
                            "    for (var i = 0; i < events.length; ++i) {\n" +
                            "      events[i](event);\n" +
                            "    }\n" +
                            "    var handler = this[\"on\" + event.type];\n" +
                            "    if (handler) handler.apply(this, [event]);\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Handles an event from Flash.\n" +
                            "   * @param {Object} flashEvent\n" +
                            "   */\n" +
                            "  WebSocket.prototype.__handleEvent = function(flashEvent) {\n" +
                            "    \n" +
                            "    if (\"readyState\" in flashEvent) {\n" +
                            "      this.readyState = flashEvent.readyState;\n" +
                            "    }\n" +
                            "    if (\"protocol\" in flashEvent) {\n" +
                            "      this.protocol = flashEvent.protocol;\n" +
                            "    }\n" +
                            "    \n" +
                            "    var jsEvent;\n" +
                            "    if (flashEvent.type == \"open\" || flashEvent.type == \"error\") {\n" +
                            "      jsEvent = this.__createSimpleEvent(flashEvent.type);\n" +
                            "    } else if (flashEvent.type == \"close\") {\n" +
                            "      jsEvent = this.__createSimpleEvent(\"close\");\n" +
                            "      jsEvent.wasClean = flashEvent.wasClean ? true : false;\n" +
                            "      jsEvent.code = flashEvent.code;\n" +
                            "      jsEvent.reason = flashEvent.reason;\n" +
                            "    } else if (flashEvent.type == \"message\") {\n" +
                            "      var data = decodeURIComponent(flashEvent.message);\n" +
                            "      jsEvent = this.__createMessageEvent(\"message\", data);\n" +
                            "    } else {\n" +
                            "      throw \"unknown event type: \" + flashEvent.type;\n" +
                            "    }\n" +
                            "    \n" +
                            "    this.dispatchEvent(jsEvent);\n" +
                            "    \n" +
                            "  };\n" +
                            "  \n" +
                            "  WebSocket.prototype.__createSimpleEvent = function(type) {\n" +
                            "    if (document.createEvent && window.Event) {\n" +
                            "      var event = document.createEvent(\"Event\");\n" +
                            "      event.initEvent(type, false, false);\n" +
                            "      return event;\n" +
                            "    } else {\n" +
                            "      return {type: type, bubbles: false, cancelable: false};\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  WebSocket.prototype.__createMessageEvent = function(type, data) {\n" +
                            "    if (window.MessageEvent && typeof(MessageEvent) == \"function\" && !window.opera) {\n" +
                            "      return new MessageEvent(\"message\", {\n" +
                            "        \"view\": window,\n" +
                            "        \"bubbles\": false,\n" +
                            "        \"cancelable\": false,\n" +
                            "        \"data\": data\n" +
                            "      });\n" +
                            "    } else if (document.createEvent && window.MessageEvent && !window.opera) {\n" +
                            "      var event = document.createEvent(\"MessageEvent\");\n" +
                            "    \tevent.initMessageEvent(\"message\", false, false, data, null, null, window, null);\n" +
                            "      return event;\n" +
                            "    } else {\n" +
                            "      // Old IE and Opera, the latter one truncates the data parameter after any 0x00 bytes.\n" +
                            "      return {type: type, data: data, bubbles: false, cancelable: false};\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  /**\n" +
                            "   * Define the WebSocket readyState enumeration.\n" +
                            "   */\n" +
                            "  WebSocket.CONNECTING = 0;\n" +
                            "  WebSocket.OPEN = 1;\n" +
                            "  WebSocket.CLOSING = 2;\n" +
                            "  WebSocket.CLOSED = 3;\n" +
                            "\n" +
                            "  // Field to check implementation of WebSocket.\n" +
                            "  WebSocket.__isFlashImplementation = true;\n" +
                            "  WebSocket.__initialized = false;\n" +
                            "  WebSocket.__flash = null;\n" +
                            "  WebSocket.__instances = {};\n" +
                            "  WebSocket.__tasks = [];\n" +
                            "  WebSocket.__nextId = 0;\n" +
                            "  \n" +
                            "  /**\n" +
                            "   * Load a new flash security policy file.\n" +
                            "   * @param {string} url\n" +
                            "   */\n" +
                            "  WebSocket.loadFlashPolicyFile = function(url){\n" +
                            "    WebSocket.__addTask(function() {\n" +
                            "      WebSocket.__flash.loadManualPolicyFile(url);\n" +
                            "    });\n" +
                            "  };\n" +
                            "\n" +
                            "  /**\n" +
                            "   * Loads WebSocketMain.swf and creates WebSocketMain object in Flash.\n" +
                            "   */\n" +
                            "  WebSocket.__initialize = function() {\n" +
                            "    \n" +
                            "    if (WebSocket.__initialized) return;\n" +
                            "    WebSocket.__initialized = true;\n" +
                            "    \n" +
                            "    if (WebSocket.__swfLocation) {\n" +
                            "      // For backword compatibility.\n" +
                            "      window.WEB_SOCKET_SWF_LOCATION = WebSocket.__swfLocation;\n" +
                            "    }\n" +
                            "    if (!window.WEB_SOCKET_SWF_LOCATION) {\n" +
                            "      logger.error(\"[WebSocket] set WEB_SOCKET_SWF_LOCATION to location of WebSocketMain.swf\");\n" +
                            "      return;\n" +
                            "    }\n" +
                            "    if (!window.WEB_SOCKET_SUPPRESS_CROSS_DOMAIN_SWF_ERROR &&\n" +
                            "        !WEB_SOCKET_SWF_LOCATION.match(/(^|\\/)WebSocketMainInsecure\\.swf(\\?.*)?$/) &&\n" +
                            "        WEB_SOCKET_SWF_LOCATION.match(/^\\w+:\\/\\/([^\\/]+)/)) {\n" +
                            "      var swfHost = RegExp.$1;\n" +
                            "      if (location.host != swfHost) {\n" +
                            "        logger.error(\n" +
                            "            \"[WebSocket] You must host HTML and WebSocketMain.swf in the same host \" +\n" +
                            "            \"('\" + location.host + \"' != '\" + swfHost + \"'). \" +\n" +
                            "            \"See also 'How to host HTML file and SWF file in different domains' section \" +\n" +
                            "            \"in README.md. If you use WebSocketMainInsecure.swf, you can suppress this message \" +\n" +
                            "            \"by WEB_SOCKET_SUPPRESS_CROSS_DOMAIN_SWF_ERROR = true;\");\n" +
                            "      }\n" +
                            "    }\n" +
                            "    var container = document.createElement(\"div\");\n" +
                            "    container.id = \"webSocketContainer\";\n" +
                            "    // Hides Flash box. We cannot use display: none or visibility: hidden because it prevents\n" +
                            "    // Flash from loading at least in IE. So we move it out of the screen at (-100, -100).\n" +
                            "    // But this even doesn't work with Flash Lite (e.g. in Droid Incredible). So with Flash\n" +
                            "    // Lite, we put it at (0, 0). This shows 1x1 box visible at left-top corner but this is\n" +
                            "    // the best we can do as far as we know now.\n" +
                            "    container.style.position = \"absolute\";\n" +
                            "    if (WebSocket.__isFlashLite()) {\n" +
                            "      container.style.left = \"0px\";\n" +
                            "      container.style.top = \"0px\";\n" +
                            "    } else {\n" +
                            "      container.style.left = \"-100px\";\n" +
                            "      container.style.top = \"-100px\";\n" +
                            "    }\n" +
                            "    var holder = document.createElement(\"div\");\n" +
                            "    holder.id = \"webSocketFlash\";\n" +
                            "    container.appendChild(holder);\n" +
                            "    document.body.appendChild(container);\n" +
                            "    // See this article for hasPriority:\n" +
                            "    // http://help.adobe.com/en_US/as3/mobile/WS4bebcd66a74275c36cfb8137124318eebc6-7ffd.html\n" +
                            "    swfobject.embedSWF(\n" +
                            "      WEB_SOCKET_SWF_LOCATION,\n" +
                            "      \"webSocketFlash\",\n" +
                            "      \"1\" /* width */,\n" +
                            "      \"1\" /* height */,\n" +
                            "      \"10.0.0\" /* SWF version */,\n" +
                            "      null,\n" +
                            "      null,\n" +
                            "      {hasPriority: true, swliveconnect : true, allowScriptAccess: \"always\"},\n" +
                            "      null,\n" +
                            "      function(e) {\n" +
                            "        if (!e.success) {\n" +
                            "          logger.error(\"[WebSocket] swfobject.embedSWF failed\");\n" +
                            "        }\n" +
                            "      }\n" +
                            "    );\n" +
                            "    \n" +
                            "  };\n" +
                            "  \n" +
                            "  /**\n" +
                            "   * Called by Flash to notify JS that it's fully loaded and ready\n" +
                            "   * for communication.\n" +
                            "   */\n" +
                            "  WebSocket.__onFlashInitialized = function() {\n" +
                            "    // We need to set a timeout here to avoid round-trip calls\n" +
                            "    // to flash during the initialization process.\n" +
                            "    setTimeout(function() {\n" +
                            "      WebSocket.__flash = document.getElementById(\"webSocketFlash\");\n" +
                            "      WebSocket.__flash.setCallerUrl(location.href);\n" +
                            "      WebSocket.__flash.setDebug(!!window.WEB_SOCKET_DEBUG);\n" +
                            "      for (var i = 0; i < WebSocket.__tasks.length; ++i) {\n" +
                            "        WebSocket.__tasks[i]();\n" +
                            "      }\n" +
                            "      WebSocket.__tasks = [];\n" +
                            "    }, 0);\n" +
                            "  };\n" +
                            "  \n" +
                            "  /**\n" +
                            "   * Called by Flash to notify WebSockets events are fired.\n" +
                            "   */\n" +
                            "  WebSocket.__onFlashEvent = function() {\n" +
                            "    setTimeout(function() {\n" +
                            "      try {\n" +
                            "        // Gets events using receiveEvents() instead of getting it from event object\n" +
                            "        // of Flash event. This is to make sure to keep message order.\n" +
                            "        // It seems sometimes Flash events don't arrive in the same order as they are sent.\n" +
                            "        var events = WebSocket.__flash.receiveEvents();\n" +
                            "        for (var i = 0; i < events.length; ++i) {\n" +
                            "          WebSocket.__instances[events[i].webSocketId].__handleEvent(events[i]);\n" +
                            "        }\n" +
                            "      } catch (e) {\n" +
                            "        logger.error(e);\n" +
                            "      }\n" +
                            "    }, 0);\n" +
                            "    return true;\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Called by Flash.\n" +
                            "  WebSocket.__log = function(message) {\n" +
                            "    logger.log(decodeURIComponent(message));\n" +
                            "  };\n" +
                            "  \n" +
                            "  // Called by Flash.\n" +
                            "  WebSocket.__error = function(message) {\n" +
                            "    logger.error(decodeURIComponent(message));\n" +
                            "  };\n" +
                            "  \n" +
                            "  WebSocket.__addTask = function(task) {\n" +
                            "    if (WebSocket.__flash) {\n" +
                            "      task();\n" +
                            "    } else {\n" +
                            "      WebSocket.__tasks.push(task);\n" +
                            "    }\n" +
                            "  };\n" +
                            "  \n" +
                            "  /**\n" +
                            "   * Test if the browser is running flash lite.\n" +
                            "   * @return {boolean} True if flash lite is running, false otherwise.\n" +
                            "   */\n" +
                            "  WebSocket.__isFlashLite = function() {\n" +
                            "    if (!window.navigator || !window.navigator.mimeTypes) {\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "    var mimeType = window.navigator.mimeTypes[\"application/x-shockwave-flash\"];\n" +
                            "    if (!mimeType || !mimeType.enabledPlugin || !mimeType.enabledPlugin.filename) {\n" +
                            "      return false;\n" +
                            "    }\n" +
                            "    return mimeType.enabledPlugin.filename.match(/flashlite/i) ? true : false;\n" +
                            "  };\n" +
                            "  \n" +
                            "  if (!window.WEB_SOCKET_DISABLE_AUTO_INITIALIZATION) {\n" +
                            "    // NOTE:\n" +
                            "    //   This fires immediately if web_socket.js is dynamically loaded after\n" +
                            "    //   the document is loaded.\n" +
                            "    swfobject.addDomLoadEvent(function() {\n" +
                            "      WebSocket.__initialize();\n" +
                            "    });\n" +
                            "  }\n" +
                            "  \n" +
                            "})();\n");
                    break;
                default:
                    h.respond(
                            "<html>\n" +
                                    "  <head>\n" +
                                    "    <title></title>\n" +
                                    "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">\n" +
                                    "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"websocket.js\"></script>\n" +
                                    "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/runtime.js\"></script>\n" +
                                    "    <script type=\"text/javascript\" charset=\"utf-8\" src=\"teavm/classes.js\"></script>\n" +
                                    "  </head>\n" +
                                    "  <body onload=\"main()\">\n" +
//                        "    <h1>Hello web application</h1>\n" +
//                        "    <button id=\"hello-button\">Hello, server!</button>\n" +
//                        "    <div id=\"response-panel\">\n" +
//                        "    </div>\n" +
//                        "    <div style=\"display:none\" id=\"thinking-panel\"><i>Server is thinking...</i></div>\n" +
                                    "  </body>\n" +
                                    "</html>");
                    break;

            }


    }

    final CustomConcurrentHashMap<WebSocket, NAR> reasoners = new CustomConcurrentHashMap<>(
            WEAK, IDENTITY, STRONG, IDENTITY, 64
    ) {
        @Override
        protected void reclaim(NAR value) {
            value.stop();
            value.reset();
        }
    };

    @Override
    public boolean wssConnect(SelectionKey key) {

        return true;
    }

    @Override
    public void wssOpen(WebSocket ws, ClientHandshake handshake) {

        if (ws.getResourceDescriptor().equals("/")) {
            ws.close(CloseFrame.REFUSE);
            return;
        }

        NAR n = reasoners.computeIfAbsent(ws, (Function<WebSocket, NAR>) this::reasoner);
        ws.setAttachment(n);

    }

    private NAR reasoner(WebSocket ws) {
        NAR n = new NARS().withNAL(1, 8).index(sharedIndexAdapter).get();

        String path = ws.getResourceDescriptor();
        assert (path.charAt(0) == '/');
        path = path.substring(1);
        n.named(path);

        int initialFPS = 5;
        n.startFPS(initialFPS);

        try {
            n.input("a:b.");
            n.input("b:c.");
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

        n.onTask(new WebSocketLogger(ws, n, initialFPS));


        return n;
    }

    public Web() {
        this.nar = NARchy.core();
        this.sharedIndexAdapter = new ProxyConceptIndex(nar.concepts);
    }

    public static void clientGenerate() {
        try {
            TeaVMTool tea = new TeaVMTool();
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/teacache"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(new File("/tmp/tea"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            tea.setMainClass(WebClientJS.class.getName());
            tea.setCacheDirectory(new File("/tmp/teacache"));
            tea.setTargetDirectory(new File("/tmp/tea"));
            tea.setLog(new TeaVMToolLog() {
                @Override
                public void info(String text) {
                    System.out.println(text);
                }

                @Override
                public void debug(String text) {
                    System.out.println(text);
                }

                @Override
                public void warning(String text) {
                    System.out.println(text);
                }

                @Override
                public void error(String text) {
                    System.out.println(text);
                }

                @Override
                public void info(String text, Throwable e) {
                    System.out.println(text);
                }

                @Override
                public void debug(String text, Throwable e) {
                    System.out.println(text);
                }

                @Override
                public void warning(String text, Throwable e) {
                    System.out.println(text);
                }

                @Override
                public void error(String text, Throwable e) {
                    System.out.println(text);
                }
            });

            //tea.setDebugInformationGenerated(true);
            //tea.setIncremental(true);

            tea.setRuntime(RuntimeCopyOperation.SEPARATE);


            tea.generate();
            System.out.println("TeaVM generated: " + tea.getGeneratedFiles());
        } catch (TeaVMToolException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        clientGenerate();

        int port;
        if (args.length > 0) {
            port = Texts.i(args[0]);
        } else {
            port = DEFAULT_PORT;
        }


        jcog.net.http.HttpServer h = new HttpServer("0.0.0.0", port, new Web());
        h.runFPS(10f);


//        Util.sleep(100);
//
//        WebClient c1 = new WebClient(URI.create("ws://localhost:60606/a"));
//        WebClient c2 = new WebClient(URI.create("ws://localhost:60606/b"));
//
//        Util.sleep(500);
//        c1.closeBlocking();
//        c2.closeBlocking();
    }

    public static class WebClient extends WebSocketClient {

        public WebClient(URI serverUri) {
            super(serverUri);
            try {
                connectBlocking();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {

        }

        @Override
        public void onMessage(String message) {
            System.out.println(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

        }

        @Override
        public void onError(Exception ex) {

        }
    }

    private static class WebSocketLogger implements Consumer<Task> {

        private final NAR n;
        private final int initialFPS;
        volatile WebSocket w;

        public WebSocketLogger(WebSocket ws, NAR n, int initialFPS) {
            this.n = n;
            this.initialFPS = initialFPS;
            w = ws;
        }

        @Override
        public void accept(Task t) {
            if (w != null && w.isOpen()) {
                if (!n.loop.isRunning())
                    n.startFPS(initialFPS);

                try {
                    w.send(t.toString(true).toString());
                } catch (Exception e) {
                    w = null;
                    n.stop();
                    w.close();
                }
            } else {

                if (n.loop.isRunning()) {
                    w = null;
                    n.stop();
                }
            }
        }
    }
}
