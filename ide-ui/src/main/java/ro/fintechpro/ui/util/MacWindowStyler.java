package ro.fintechpro.ui.util;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Map;

public class MacWindowStyler {

    public interface Cocoa extends Library {
        // Function Mapper: Maps any method starting with "send" to the native "objc_msgSend"
        Map<String, Object> OPTIONS = Map.of(
                Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (library, method) -> {
                    if (method.getName().startsWith("send")) {
                        return "objc_msgSend";
                    }
                    return method.getName();
                }
        );

        Cocoa INSTANCE = Native.load("Cocoa", Cocoa.class, OPTIONS);

        // --- Standard Native Functions ---
        Pointer objc_getClass(String className);
        Pointer sel_registerName(String selectorName);

        // --- STRICTLY TYPED 'objc_msgSend' MAPPINGS ---
        // These replace the generic Object... args to prevent crashes on Apple Silicon

        // 1. Get a pointer (e.g., sharedApplication, windows, title)
        Pointer sendPointer(Pointer self, Pointer op);

        // 2. Get a pointer with 1 pointer arg (e.g., stringWithUTF8String:)
        Pointer sendPointer(Pointer self, Pointer op, String arg);
        Pointer sendPointer(Pointer self, Pointer op, Pointer arg);

        // 3. Get a pointer from an INDEX (CRITICAL FIX for objectAtIndex:)
        Pointer sendPointerAtIndex(Pointer self, Pointer op, long index);

        // 4. Get a long value (e.g., count, styleMask)
        long sendLong(Pointer self, Pointer op);

        // 5. Void Setters
        void sendVoidLong(Pointer self, Pointer op, long value);    // setStyleMask:
        void sendVoidBool(Pointer self, Pointer op, boolean value); // setTitlebarAppearsTransparent:
        void sendVoidPtr(Pointer self, Pointer op, Pointer value);  // setTitle:
    }

    public static void makeTitleBarTransparent(Stage stage) {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) return;

        // Ensure we run after the window peer is created
        if (stage.isShowing()) {
            Platform.runLater(() -> applyMacStyle(stage));
        } else {
            stage.setOnShown(e -> Platform.runLater(() -> applyMacStyle(stage)));
        }
    }

    private static void applyMacStyle(Stage stage) {
        Cocoa cocoa = Cocoa.INSTANCE;
        String searchTitle = stage.getTitle();

        // Safety check
        if (searchTitle == null) searchTitle = "";

        System.out.println("MacWindowStyler: Searching for window '" + searchTitle + "'");
        Pointer nsWindow = getWindowPointer(searchTitle);

        if (nsWindow == null) {
            System.err.println("MacWindowStyler: Window not found.");
            return;
        }

        System.out.println("MacWindowStyler: Window found. Applying styles...");

        try {
            Pointer setStyleMask = cocoa.sel_registerName("setStyleMask:");
            Pointer styleMask = cocoa.sel_registerName("styleMask");
            Pointer setTransparent = cocoa.sel_registerName("setTitlebarAppearsTransparent:");
            Pointer setTitleVis = cocoa.sel_registerName("setTitleVisibility:");
            Pointer setMovable = cocoa.sel_registerName("setMovableByWindowBackground:");
            Pointer setTitle = cocoa.sel_registerName("setTitle:");

            // 1. Get Current Style
            long currentStyle = cocoa.sendLong(nsWindow, styleMask);

            // 2. Construct New Style
            // NSWindowStyleMaskFullSizeContentView = 1 << 15 (32768)
            // NSWindowStyleMaskTitled = 1 << 0
            // NSWindowStyleMaskResizable = 1 << 3
            long newStyle = currentStyle | (1 << 15) | (1 << 0) | (1 << 3);

            // 3. Apply Style (using strict long setter)
            cocoa.sendVoidLong(nsWindow, setStyleMask, newStyle);

            // 4. Set Attributes (using strict boolean setters)
            cocoa.sendVoidBool(nsWindow, setTransparent, true);
            cocoa.sendVoidBool(nsWindow, setMovable, true);
            cocoa.sendVoidLong(nsWindow, setTitleVis, 1L); // Hide Title Text

            // 5. Clear Native Title String
            Pointer emptyString = cocoa.sendPointer(
                    cocoa.objc_getClass("NSString"),
                    cocoa.sel_registerName("stringWithUTF8String:"),
                    ""
            );
            cocoa.sendVoidPtr(nsWindow, setTitle, emptyString);

            System.out.println("MacWindowStyler: Success.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Pointer getWindowPointer(String title) {
        Cocoa cocoa = Cocoa.INSTANCE;

        // Get Shared Application
        Pointer app = cocoa.sendPointer(
                cocoa.objc_getClass("NSApplication"),
                cocoa.sel_registerName("sharedApplication")
        );

        // Get Windows Array
        Pointer windows = cocoa.sendPointer(app, cocoa.sel_registerName("windows"));

        // Get Count (Strict Long)
        long count = cocoa.sendLong(windows, cocoa.sel_registerName("count"));

        Pointer objectAtIndex = cocoa.sel_registerName("objectAtIndex:");
        Pointer titleSel = cocoa.sel_registerName("title");
        Pointer utf8String = cocoa.sel_registerName("UTF8String");

        for (long i = 0; i < count; i++) {
            // --- CRITICAL FIX IS HERE ---
            // Use the specific method signature that takes a 'long index'.
            // This prevents JNA from passing garbage data to the native side.
            Pointer win = cocoa.sendPointerAtIndex(windows, objectAtIndex, i);
            // ----------------------------

            Pointer nsTitle = cocoa.sendPointer(win, titleSel);
            if (nsTitle != null) {
                // UTF8String returns a (char*), which is a Pointer
                Pointer utf8TitlePtr = cocoa.sendPointer(nsTitle, utf8String);

                if (utf8TitlePtr != null) {
                    String winTitle = utf8TitlePtr.getString(0);
                    if (title.equals(winTitle)) {
                        return win;
                    }
                }
            }
        }
        return null;
    }
}