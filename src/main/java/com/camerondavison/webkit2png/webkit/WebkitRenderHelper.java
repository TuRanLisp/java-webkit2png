package com.camerondavison.webkit2png.webkit;

import com.trolltech.qt.core.*;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QPixmap;
import com.trolltech.qt.network.QNetworkReply;
import com.trolltech.qt.network.QSslError;
import com.trolltech.qt.webkit.QWebPage;
import com.trolltech.qt.webkit.QWebSettings;
import com.trolltech.qt.webkit.QWebView;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * User: loyd
 * Date: 7/28/11
 */
public class WebkitRenderHelper extends QObject {
    private static final Logger log = Logger.getLogger(WebkitRenderHelper.class);

    private final QWebPage page;
    private final QWebView view;
    private final QMainWindow window;

    transient private boolean loading;
    transient private boolean loadingResult;
    
    // Only should be called from inside this package
    WebkitRenderHelper(WebkitRenderer parent) {
        page = new QWebPage();
        view = new QWebView();
        view.setPage(page);
        window = new QMainWindow();
        window.setCentralWidget(view);

        // Import QWebSettings
        for (Map.Entry<QWebSettings.WebAttribute, Boolean> attr : parent.getAttributes().entrySet()) {
            page.settings().setAttribute(attr.getKey(), attr.getValue());
        }

        // Connect required event listeners
        page.loadFinished.connect(this, "onLoadFinished(boolean)");
        page.loadStarted.connect(this, "onLoadStarted()");
        page.networkAccessManager().sslErrors.connect(this, "onSslErrors(com.trolltech.qt.network.QNetworkReply, java.util.List)");

        // The way we will use this, it seems to be unesseccary to have Scrollbars enabled
        page.mainFrame().setScrollBarPolicy(Qt.Orientation.Horizontal, Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
        page.mainFrame().setScrollBarPolicy(Qt.Orientation.Vertical, Qt.ScrollBarPolicy.ScrollBarAlwaysOff);

        page.settings().setUserStyleSheetUrl(new QUrl("data:text/css,html,body{overflow-y:hidden !important;}"));

        // Show this widget
        window.show();
    }

    public QPixmap render(QUrl url,
                          int width, int height, int timeout, int wait, boolean grabWholeWindow,
                          int scaleToWidth, int scaleToHeight, String scaleRatio
    ) {
        loadPage(url, width, height, timeout);

        // Wait for end of timer. In this time, process
        // other outstanding Qt events.
        if (wait > 0) {
            log.debug("Waiting " + wait + " ms");
            final long waitToTime = System.currentTimeMillis() + wait;
            while (System.currentTimeMillis() < waitToTime) {
                while (QApplication.hasPendingEvents()) {
                    QApplication.processEvents();
                }
            }
        }

        // Paint this frame into an image
        //self._window.repaint()
        while (QApplication.hasPendingEvents()) {
            QApplication.processEvents();
        }

        // todo(cameron) renderTransparentBackground

        final QPixmap image;
        if (grabWholeWindow) {
            // Note that this does not fully ensure that the
            // window still has the focus when the screen is
            // grabbed. This might result in a race condition.
            view.activateWindow();
            image = QPixmap.grabWindow(window.winId());
        } else {
            image = QPixmap.grabWidget(window);
        }

        return postProcessImage(image, scaleToWidth, scaleToHeight, scaleRatio);
    }

    /**
     * If 'scaleToWidth' or 'scaleToHeight' are set to a value
     * greater than zero this method will scale the image
     * using the method defined in 'scaleRatio'.
     * @param qImage
     * @param scaleToWidth
     * @param scaleToHeight
     * @param scaleRatio
     * @return
     */
    private QPixmap postProcessImage(QPixmap qImage, int scaleToWidth, int scaleToHeight, String scaleRatio) {
        if (scaleToWidth > 0 || scaleToHeight > 0) {
            final Qt.AspectRatioMode ratio;
            // Scale this image
            if ("keep".equals(scaleRatio)) {
                ratio = Qt.AspectRatioMode.KeepAspectRatio;
            } else if ("expand".equals(scaleRatio) || "crop".equals("crop")) {
                ratio = Qt.AspectRatioMode.KeepAspectRatioByExpanding;
            } else { // 'ignore'
                ratio = Qt.AspectRatioMode.IgnoreAspectRatio;
            }
            qImage = qImage.scaled(scaleToWidth, scaleToHeight, ratio);
            if ("crop".equals(scaleRatio)) {
                qImage = qImage.copy(0, 0, scaleToWidth, scaleToHeight);
            }
        }
        return qImage;
    }

    /**
     * This method implements the logic for retrieving and displaying the requested page.
     * @param url
     * @param width
     * @param height
     * @param timeout
     */
    private void loadPage(QUrl url, int width, int height, int timeout) {
        // This is an event-based application. So we have to wait until
        // "loadFinished(bool)" raised.
        long cancelAt = System.currentTimeMillis() + timeout;
        loading = true;
        loadingResult = false; // Default
        
        // TODO: fromEncoded() needs to be used in some situations.  Some
        // sort of flag should be passed in to WebkitRenderer maybe?
        // self._page.mainFrame().load(QUrl.fromEncoded(url))

        page.mainFrame().load(url);
        while (loading) {
            if (timeout > 0 && System.currentTimeMillis() >= cancelAt) {
                throw new RuntimeException("Request timed out on " + url);
            }
            while (QApplication.hasPendingEvents()) {
                QCoreApplication.processEvents();
            }
        }

        log.debug("Processing result");

        if (!loadingResult) {
            log.warn("Failed to load " + url);
        }

        // Set initial viewport (the size of the "window")
        final QSize size = page.mainFrame().contentsSize();
        log.debug("contentsSize: " + size);

        if (width > 0) {
            size.setWidth(width);
        }
        if (height > 0) {
            size.setHeight(height);
        }

        window.resize(size);
    }

    public void onLoadFinished(boolean result) {
        log.debug("loading finished with result "+result);
        loading = false;
        loadingResult = result;
    }

    public void onLoadStarted() {
        log.debug("loading started");
        loading = true;
    }

    public void onSslErrors(QNetworkReply reply, List<QSslError> errors) {
        for (QSslError e : errors) {
            log.warn("SSL: " + e.errorString());
        }
        reply.ignoreSslErrors();
    }
}
