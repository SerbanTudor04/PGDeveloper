package ro.fintechpro.ui.util;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.stage.Stage;

import java.util.Map;

public class MacWindowStyler {

    public interface Cocoa extends Library {
        // --- CONFIGURATION START ---
        // Map multiple Java method names to the single real native function "objc_msgSend"
        Map<String, Object> OPTIONS = Map.of(
                Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (library, method) -> {
                    String name = method.getName();
                    if (name.equals("objc_msgSend_long") ||
                            name.equals("objc_msgSend_bool") ||
                            name.equals("objc_msgSend_atIndex")) { // Map this specific helper too
                        return "objc_msgSend";
                    }
                    return name;
                }
        );

        Cocoa INSTANCE = Native.load("Cocoa", Cocoa.class, OPTIONS);
        // --- CONFIGURATION END ---

        long NSWindowStyleMaskFullSizeContentView = 1 << 15;

        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);

        // Standard signature
        Pointer objc_msgSend(Pointer receiver, Pointer selector, Object... args);

        // Typed signatures to force correct native types
        long objc_msgSend_long(Pointer receiver, Pointer selector, Object... args);
        boolean objc_msgSend_bool(Pointer receiver, Pointer selector, Object... args);

        // EXPLICIT SIGNATURE for array access
        // This forces the 3rd argument to be a 64-bit long, preventing the crash.
        Pointer objc_msgSend_atIndex(Pointer receiver, Pointer selector, long index);
    }

    public static void makeTitleBarTransparent(Stage stage) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) return;

        if (!stage.isShowing()) {
            stage.setOnShown(e -> applyMacStyle(stage));
        } else {
            applyMacStyle(stage);
        }
    }

    private static void applyMacStyle(Stage stage) {
        Cocoa cocoa = Cocoa.INSTANCE;

        Pointer nsWindow = getWindowPointer(stage.getTitle());
        if (nsWindow == null) return;

        Pointer setStyleMaskSel = cocoa.sel_registerName("setStyleMask:");
        Pointer styleMaskSel = cocoa.sel_registerName("styleMask");
        Pointer setTitlebarAppearsTransparentSel = cocoa.sel_registerName("setTitlebarAppearsTransparent:");
        Pointer setTitleVisibilitySel = cocoa.sel_registerName("setTitleVisibility:");
        Pointer setMovableByWindowBackgroundSel = cocoa.sel_registerName("setMovableByWindowBackground:");

        // 1. Get current mask
        long currentStyle = cocoa.objc_msgSend_long(nsWindow, styleMaskSel);

        // 2. Apply Full Size Content View
        long newStyle = currentStyle | Cocoa.NSWindowStyleMaskFullSizeContentView;
        cocoa.objc_msgSend(nsWindow, setStyleMaskSel, newStyle);

        // 3. Apply Styling
        cocoa.objc_msgSend(nsWindow, setTitlebarAppearsTransparentSel, true);
        cocoa.objc_msgSend(nsWindow, setTitleVisibilitySel, 1L); // 1 = NSWindowTitleHidden
        cocoa.objc_msgSend(nsWindow, setMovableByWindowBackgroundSel, true);
    }

    private static Pointer getWindowPointer(String title) {
        Cocoa cocoa = Cocoa.INSTANCE;

        Pointer app = cocoa.objc_msgSend(cocoa.objc_getClass("NSApplication"), cocoa.sel_registerName("sharedApplication"));
        Pointer windows = cocoa.objc_msgSend(app, cocoa.sel_registerName("windows"));

        long count = cocoa.objc_msgSend_long(windows, cocoa.sel_registerName("count"));

        Pointer objectAtIndex = cocoa.sel_registerName("objectAtIndex:");
        Pointer titleSel = cocoa.sel_registerName("title");
        Pointer utf8StringSel = cocoa.sel_registerName("UTF8String");

        for (long i = 0; i < count; i++) {
            // CRITICAL FIX: Use the specific method that enforces 'long' (64-bit) for the index
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