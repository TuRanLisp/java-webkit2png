package com.camerondavison.webkit2png.tools;

import com.camerondavison.webkit2png.webkit.WebkitRenderer;
import com.trolltech.qt.core.QFile;
import com.trolltech.qt.core.QIODevice;
import com.trolltech.qt.gui.QApplication;

/**
 * User: loyd
 * Date: 7/28/11
 */
public class WebKitMain {
    static {
        
    }

    public static void main(String args[]) {
        QApplication.initialize(args);

        final WebkitRenderer renderer = new WebkitRenderer();
        final QFile qFile = new QFile("/tmp/java.png");
        qFile.open(QIODevice.OpenModeFlag.WriteOnly, QIODevice.OpenModeFlag.Truncate);

        renderer.renderToFile("http://www.bestbuy.com", qFile);
        qFile.close();
    }
}
