package ro.fintechpro.ui.util;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Map;

public class MacWindowStyler {

    // Define the native interface strictly
    public interface Cocoa extends Library {
        Map<String, Object> OPTIONS = Map.of(
                Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (library, method) -> {
                    String name = method.getName();
                    // Map all our Java helper names to the real native function
                    if (name.equals("sendInt") || name.equals("sendLong") ||
                            name.equals("sendVoid") || name.equals("sendVoidBool") ||
                            name.equals("sendVoidLong") || name.equals("sendPointer")) {
                        return "objc_msgSend";
                    }
                    return name;
                }
        );

        Cocoa INSTANCE = Native.load("Cocoa", Cocoa.class, OPTIONS);

        // Constants
        long NSWindowStyleMaskTitled = 1 << 0;
        long NSWindowStyleMaskResizable = 1 << 3;
        long NSWindowStyleMaskFullSizeContentView = 1 << 15;

        // Selectors
        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);

        // --- EXPLICIT SIGNATURES (No variable arguments to avoid crashes) ---

        // 1. Get a Pointer (e.g., getting a Window or String)
        Pointer sendPointer(Pointer receiver, Pointer selector, Object... args);

        // 2. Get a Long/Int (e.g., styleMask, count)
        long sendLong(Pointer receiver, Pointer selector);
        long sendLong(Pointer receiver, Pointer selector, long arg1); // Overload for methods taking 1 arg

        // 3. Void returns (Setters)
        // Set a long value (setStyleMask:)
        void sendVoidLong(Pointer receiver, Pointer selector, long arg1);

        // Set a boolean value (setTitlebarAppearsTransparent:)
        // Note: MacOS BOOL is strictly a byte (signed char). JNA handles boolean -> byte mapping generally,
        // but explicit mapping is safer if crashes persist.
        void sendVoidBool(Pointer receiver, Pointer selector, boolean arg1);
    }

    public static void makeTitleBarTransparent(Stage stage) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) return;

        if (stage.isShowing()) {
            Platform.runLater(() -> applyMacStyle(stage));
        } else {
            stage.setOnShown(e -> Platform.runLater(() -> applyMacStyle(stage)));
        }
    }

    private static void applyMacStyle(Stage stage) {
        Cocoa cocoa = Cocoa.INSTANCE;
        String searchTitle = stage.getTitle();

        System.out.println("MacWindowStyler: Searching for window '" + searchTitle + "'");
        Pointer nsWindow = getWindowPointer(searchTitle);

        if (nsWindow == null) {
            System.err.println("MacWindowStyler: Window not found via native API.");
            return;
        }

        System.out.println("MacWindowStyler: Window found. Applying style mask...");

        try {
            Pointer setStyleMask = cocoa.sel_registerName("setStyleMask:");
            Pointer styleMask = cocoa.sel_registerName("styleMask");
            Pointer setTransparent = cocoa.sel_registerName("setTitlebarAppearsTransparent:");
            Pointer setTitleVis = cocoa.sel_registerName("setTitleVisibility:");
            Pointer setMovable = cocoa.sel_registerName("setMovableByWindowBackground:");
            Pointer setTitle = cocoa.sel_registerName("setTitle:");

            // 1. Get Current Mask
            long currentStyle = cocoa.sendLong(nsWindow, styleMask);

            // 2. Set New Mask (Add FullSizeContentView + Titled + Resizable)
            long newStyle = currentStyle | Cocoa.NSWindowStyleMaskFullSizeContentView
                    | Cocoa.NSWindowStyleMaskTitled
                    | Cocoa.NSWindowStyleMaskResizable;

            cocoa.sendVoidLong(nsWindow, setStyleMask, newStyle);

            // 3. Set Properties
            cocoa.sendVoidBool(nsWindow, setTransparent, true);
            cocoa.sendVoidBool(nsWindow, setMovable, true);
            cocoa.sendVoidLong(nsWindow, setTitleVis, 1L); // 1 = NSWindowTitleHidden

            // 4. Clear Title String
            Pointer emptyString = cocoa.sendPointer(
                    cocoa.objc_getClass("NSString"),
                    cocoa.sel_registerName("stringWithUTF8String:"),
                    ""
            );
            cocoa.sendPointer(nsWindow, setTitle, emptyString);

            System.out.println("MacWindowStyler: Success.");

        } catch (Exception e) {
            System.err.println("MacWindowStyler: Error applying styles - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Pointer getWindowPointer(String title) {
        if (title == null) return null;
        Cocoa cocoa = Cocoa.INSTANCE;

        Pointer app = cocoa.sendPointer(cocoa.objc_getClass("NSApplication"), cocoa.sel_registerName("sharedApplication"));
        Pointer windows = cocoa.sendPointer(app, cocoa.sel_registerName("windows"));
        long count = cocoa.sendLong(windows, cocoa.sel_registerName("count"));

        Pointer objectAtIndex = cocoa.sel_registerName("objectAtIndex:");
        Pointer titleSel = cocoa.sel_registerName("title");
        Pointer utf8String = cocoa.sel_registerName("UTF8String");

        for (long i = 0; i < count; i++) {
            // Use 'sendPointer' with arguments for array access.
            // We cast 'i' to Long explicitly in the args to match native expectation if using varargs,
            // but strict mapping is better:
            Pointer win = cocoa.sendPointer(windows, objectAtIndex, i);

            Pointer nsTitle = cocoa.sendPointer(win, titleSel);
            if (nsTitle != null) {
                Pointer utf8Title = cocoa.sendPointer(nsTitle, utf8String);
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