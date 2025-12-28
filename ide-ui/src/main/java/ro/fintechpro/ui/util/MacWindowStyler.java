package ro.fintechpro.ui.util;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.stage.Stage;

import java.util.Map;

public class MacWindowStyler {

    public interface Cocoa extends Library {
        // Map all these java method names to the single native "objc_msgSend" function
        Map<String, Object> OPTIONS = Map.of(
                Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (library, method) -> {
                    String name = method.getName();
                    if (name.startsWith("objc_msgSend")) {
                        return "objc_msgSend";
                    }
                    return name;
                }
        );

        Cocoa INSTANCE = Native.load("Cocoa", Cocoa.class, OPTIONS);

        long NSWindowStyleMaskFullSizeContentView = 1 << 15;

        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);

        // --- TYPED SIGNATURES (Critical for Apple Silicon) ---
        // Generic (careful with primitives here)
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Object... args);

        // For getting the current style (returns long)
        long objc_msgSend_long(Pointer receiver, Pointer selector, Object... args);

        // For SETTING the style (returns void, takes long) - FIXES THE BUG
        void objc_msgSend_v_long(Pointer receiver, Pointer selector, long arg1);

        // For setting boolean properties (setTitlebarAppearsTransparent)
        void objc_msgSend_v_bool(Pointer receiver, Pointer selector, boolean arg1);

        // For array access (takes long index)
        Pointer objc_msgSend_atIndex(Pointer receiver, Pointer selector, long index);
    }

    public static void makeTitleBarTransparent(Stage stage) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) return;

        // Apply immediately if showing, otherwise wait
        if (stage.isShowing()) {
            applyMacStyle(stage);
        } else {
            stage.setOnShown(e -> applyMacStyle(stage));
        }
    }

    private static void applyMacStyle(Stage stage) {
        Cocoa cocoa = Cocoa.INSTANCE;

        Pointer nsWindow = getWindowPointer(stage.getTitle());
        if (nsWindow == null) {
            System.err.println("MacWindowStyler: Could not find NSWindow. Ensure Stage has a title.");
            return;
        }

        Pointer setStyleMaskSel = cocoa.sel_registerName("setStyleMask:");
        Pointer styleMaskSel = cocoa.sel_registerName("styleMask");
        Pointer setTitlebarAppearsTransparentSel = cocoa.sel_registerName("setTitlebarAppearsTransparent:");
        Pointer setTitleVisibilitySel = cocoa.sel_registerName("setTitleVisibility:");
        Pointer setMovableByWindowBackgroundSel = cocoa.sel_registerName("setMovableByWindowBackground:");

        // 1. Get current style
        long currentStyle = cocoa.objc_msgSend_long(nsWindow, styleMaskSel);

        // 2. Add FullSizeContentView (allows content to flow behind titlebar)
        long newStyle = currentStyle | Cocoa.NSWindowStyleMaskFullSizeContentView;

        // USE TYPED METHOD to ensure the long value is passed correctly
        cocoa.objc_msgSend_v_long(nsWindow, setStyleMaskSel, newStyle);

        // 3. Make title bar transparent and hide text
        cocoa.objc_msgSend_v_bool(nsWindow, setTitlebarAppearsTransparentSel, true);
        cocoa.objc_msgSend_v_bool(nsWindow, setMovableByWindowBackgroundSel, true);
        cocoa.objc_msgSend_v_long(nsWindow, setTitleVisibilitySel, 1L); // 1 = NSWindowTitleHidden
    }

    private static Pointer getWindowPointer(String title) {
        if (title == null) return null;

        Cocoa cocoa = Cocoa.INSTANCE;
        Pointer app = cocoa.objc_msgSend(cocoa.objc_getClass("NSApplication"), cocoa.sel_registerName("sharedApplication"));
        Pointer windows = cocoa.objc_msgSend(app, cocoa.sel_registerName("windows"));
        long count = cocoa.objc_msgSend_long(windows, cocoa.sel_registerName("count"));

        Pointer objectAtIndex = cocoa.sel_registerName("objectAtIndex:");
        Pointer titleSel = cocoa.sel_registerName("title");
        Pointer utf8StringSel = cocoa.sel_registerName("UTF8String");

        for (long i = 0; i < count; i++) {
            Pointer win = cocoa.objc_msgSend_atIndex(windows, objectAtIndex, i);
            Pointer nsTitle = cocoa.objc_msgSend(win, titleSel);

            if (nsTitle != null) {
                Pointer utf8Title = cocoa.objc_msgSend(nsTitle, utf8StringSel);
                if (utf8Title != null) {
                    String winTitle = utf8Title.getString(0);
                    if (title.equals(winTitle)) {
                        return win;
                    }
                }
            }
        }
        return null;
    }
}