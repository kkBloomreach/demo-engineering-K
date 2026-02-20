package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

import java.net.URLDecoder;

public class DiscoveryQueryTest {

    public static void main (String[] args) {
        DiscoveryQueryTest tester;

        tester = new DiscoveryQueryTest ();
        try {
            tester.doRun ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    private DiscoveryQueryTest () {
    }

    private void doRun () throws Exception {
        DiscoveryQuery dQuery;
        String pid;
        String q;

        dQuery = new DiscoveryQuery ();
        //q = "round-graywash-sienna-dining-collection";
        q = "clear+acrylic+and+gold+morris+counter+stools+set+of+2";
        pid = dQuery.getPid (q);
        System.out.println ("pid = " + pid);
    }
}


