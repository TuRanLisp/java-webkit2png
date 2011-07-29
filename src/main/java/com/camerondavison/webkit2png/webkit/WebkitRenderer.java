package com.camerondavison.webkit2png.webkit;

import com.google.common.collect.Maps;
import com.trolltech.qt.core.QBuffer;
import com.trolltech.qt.core.QFile;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.webkit.QWebSettings;

import java.util.Map;

/**
 * User: loyd
 * Date: 7/28/11
 */
public class WebkitRenderer extends QObject {
    final Map<QWebSettings.WebAttribute, Boolean> attributes;

    public WebkitRenderer() {
        if (QApplication.instance() == null) {
            throw new RuntimeException("Unable to find QApplication instance");
        }

        attributes = Maps.newHashMap();
        attributes.put(QWebSettings.WebAttribute.JavascriptEnabled, true);
        attributes.put(QWebSettings.WebAttribute.PluginsEnabled, false);
        attributes.put(QWebSettings.WebAttribute.PrivateBrowsingEnabled, true);
        attributes.put(QWebSettings.WebAttribute.JavascriptCanOpenWindows, false);
    }

    public Map<QWebSettings.WebAttribute, Boolean> getAttributes() {
        return attributes;
    }

    private QPixmap render(String url,
                       int width, int height, int timeout, int wait, boolean grapWholeWindow,
                       int scaleToWidth, int scaleToHeight, String scaleRatio
    ) {
        // We have to use this helper object because
        // QApplication.processEvents may be called, causing
        // this method to get called while it has not returned yet
        final WebkitRenderHelper helper = new WebkitRenderHelper(this);
        final QPixmap image = helper.render(new QUrl(url),
                width, height, timeout, wait, grapWholeWindow,
                scaleToWidth, scaleToHeight, scaleRatio
        );

        // Bind helper instance to this image to prevent the
        // object from being cleaned up (and with it the QWebPage, etc)
        // before the data has been used.
        // todo(cameron) below should not be commented out, does java have this problem?
        // image.helper = helper

        return image;
    }

    /**
     * Renders the image into a File resource.
     * Returns the size of the data that has been written.
     * @param url
     * @param file
     * @return
     */
    public long renderToFile(String url, QFile file) {
        // todo(cameron) is this "PNG"?
        // format = self.format // this may not be constant due to processEvents()
        final String format = "png";

        final QPixmap image = render(url, 0, 0, 0, 0, false, 0, 0, "ignore");
        final QBuffer qBuffer = new QBuffer();
        image.save(qBuffer, format);
        file.write(qBuffer.data());
        return qBuffer.size();
    }

}
