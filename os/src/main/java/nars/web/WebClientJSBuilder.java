package nars.web;

import static nars.web.util.ClientBuilder.rebuild;

public class WebClientJSBuilder {

    public static void main(String[] args) {
        rebuild(WebClientJS.class, false);
    }

}
