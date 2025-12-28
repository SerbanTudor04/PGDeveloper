package ro.fintechpro.ui.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import javafx.stage.Stage;

public class MacWindowStyler {

    // --- JNA INTERFACE TO COCOA ---
    public interface Cocoa extends Library {
        Cocoa INSTANCE = Native.load("Cocoa", Cocoa.class);

        // Basic NSWindow selector methods
        long NSWindowStyleMaskFullSizeContentView = 1 << 15;

        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Object... args);
    }

    // --- PUBLIC API ---
    public static void makeTitleBarTransparent(Stage stage) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) return;

        // We need the window to be visible to find its ID
        if (!stage.isShowing()) {
            stage.setOnShown(e -> applyMacStyle(stage));
        } else {
            applyMacStyle(stage);
        }
    }

    private static void applyMacStyle(Stage stage) {
        Cocoa cocoa = Cocoa.INSTANCE;

        // 1. Get the NSWindow Pointer
        Pointer nsWindow = getWindowPointer(stage.getTitle());
        if (nsWindow == null) {
            System.err.println("Could not find NSWindow for styling.");
            return;
        }

        // 2. Prepare Selectors
        Pointer styleMaskSel = cocoa.sel_registerName("styleMask");
        Pointer setStyleMaskSel = cocoa.sel_registerName("setStyleMask:");
        Pointer setTitlebarAppearsTransparentSel = cocoa.sel_registerName("setTitlebarAppearsTransparent:");
        Pointer setTitleVisibilitySel = cocoa.sel_registerName("setTitleVisibility:");
        Pointer setMovableByWindowBackgroundSel = cocoa.sel_registerName("setMovableByWindowBackground:");

        // 3. Get Current Style Mask
        // FIX: Extract value from Pointer. JNA returns null if value is 0.
        Pointer stylePtr = cocoa.objc_msgSend(nsWindow, styleMaskSel);
        long currentStyle = (stylePtr == null) ? 0 : Pointer.nativeValue(stylePtr);

        // 4. Apply "Full Size Content View" (Content flows behind titlebar)
        long newStyle = currentStyle | Cocoa.NSWindowStyleMaskFullSizeContentView;
        cocoa.objc_msgSend(nsWindow, setStyleMaskSel, newStyle);

        // 5. Make Titlebar Transparent & Hide Native Text
        cocoa.objc_msgSend(nsWindow, setTitlebarAppearsTransparentSel, true);
        cocoa.objc_msgSend(nsWindow, setTitleVisibilitySel, 1); // 1 = NSWindowTitleHidden

        // 6. Optional: Allow dragging window by clicking background
        cocoa.objc_msgSend(nsWindow, setMovableByWindowBackgroundSel, true);
    }

    /**
     * Finds the NSWindow pointer by iterating the shared application's windows.
     */
    private static Pointer getWindowPointer(String title) {
        Cocoa cocoa = Cocoa.INSTANCE;

        Pointer clsNSApplication = cocoa.objc_getClass("NSApplication");
        Pointer app = cocoa.objc_msgSend(clsNSApplication, cocoa.sel_registerName("sharedApplication"));
        Pointer windows = cocoa.objc_msgSend(app, cocoa.sel_registerName("windows"));

        // "count" of the NSArray
        // FIX: Extract value from Pointer
        Pointer countPtr = cocoa.objc_msgSend(windows, cocoa.sel_registerName("count"));
        long count = (countPtr == null) ? 0 : Pointer.nativeValue(countPtr);

        Pointer objectAtIndex = cocoa.sel_registerName("objectAtIndex:");
        Pointer titleSel = cocoa.sel_registerName("title");
        Pointer utf8StringSel = cocoa.sel_registerName("UTF8String");

        for (long i = 0; i < count; i++) {
            Pointer win = cocoa.objc_msgSend(windows, objectAtIndex, i);
            Pointer nsTitle = cocoa.objc_msgSend(win, titleSel);

            if (nsTitle != null) {
                Pointer utf8Title = cocoa.objc_msgSend(nsTitle, utf8StringSel);
                if (utf8Title != null) {
                    String winTitle = utf8Title.getString(0);
                    if (winTitle.equals(title)) {
                        return win;
                    }
                }
            }
        }
        return null;
    }
}