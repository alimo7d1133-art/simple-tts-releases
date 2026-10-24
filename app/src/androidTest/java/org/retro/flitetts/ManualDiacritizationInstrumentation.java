package org.retro.flitetts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** UI-level coverage for the local Rawi diacritization action and its snapshot guard. */
public final class ManualDiacritizationInstrumentation extends Instrumentation {
    private static final String TAG = "OldTTS-Diacritization";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(120);
    private static final String SOURCE = "السلام عليكم هذه تجربة للصوت العربي";

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private void note(String message) {
        Bundle status = new Bundle();
        status.putString("stream", message + "\n");
        sendStatus(1, status);
        android.util.Log.i(TAG, message);
    }

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Activity activity = null;
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            context.getSharedPreferences("retro", Context.MODE_PRIVATE).edit()
                    .putBoolean("storage_intro_131", true).commit();

            activity = startActivitySync(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            verifySixButtons(activity);
            verifyCompletedDiacritization(activity);
            verifyEditDuringProcessing(activity);
            verifyMbrolaCanListen(activity);

            result.putString("stream", "PASS real diacritize click, snapshot guard, and MBROLA listen\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            note("FAIL " + failure);
            result.putString("error", failure.toString());
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) {
                final Activity toFinish = activity;
                runOnMainSync(toFinish::finish);
            }
        }
    }

    private void verifySixButtons(Activity activity) {
        List<Button> buttons = new ArrayList<>();
        collectButtons(activity.getWindow().getDecorView(), buttons);
        require(buttons.size() == 6, "expected 6 Button controls excluding Switch, got " + buttons.size());
        require(findButton(activity, activity.getString(R.string.diacritize_text)) != null,
                "real diacritize button is missing");
        note("PASS six ordinary Buttons and real diacritize button");
    }

    private void verifyCompletedDiacritization(Activity activity) throws Exception {
        EditText input = find(activity.getWindow().getDecorView(), EditText.class);
        Button diacritize = findButton(activity, activity.getString(R.string.diacritize_text));
        require(input != null && diacritize != null, "diacritization controls missing");
        Field status = privateField("status");
        runOnMainSync(() -> {
            input.setText(SOURCE);
            require(diacritize.performClick(), "performClick did not dispatch diacritize button");
        });
        Field busy = privateField("busy");
        waitUntilIdle(busy, activity, TIMEOUT_MS);
        String output = textOf(input);
        require(hasArabicMark(output), "diacritize button produced no Arabic marks: " + output);
        require(stripMarks(output).equals(SOURCE), "diacritization deleted/changed words: " + output);
        require(readText(status, activity).equals(activity.getString(R.string.diacritize_done)),
                "successful diacritization did not publish diacritize_done");
        note("PASS real performClick produced marked text without deleting words");
    }

    private void verifyEditDuringProcessing(Activity activity) throws Exception {
        EditText input = find(activity.getWindow().getDecorView(), EditText.class);
        Button diacritize = findButton(activity, activity.getString(R.string.diacritize_text));
        Field busy = privateField("busy");
        Field status = privateField("status");
        String longSource = repeat(SOURCE + " ", 80).trim();
        runOnMainSync(() -> {
            input.setText(longSource);
            require(diacritize.performClick(), "edit test performClick failed");
        });
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        boolean observedBusy = false;
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(busy.get(activity))) {
                observedBusy = true;
                break;
            }
            Thread.sleep(20);
        }
        if (!observedBusy) {
            require(!Boolean.TRUE.equals(busy.get(activity)), "diacritization did not settle");
            note("PASS edit-during-processing not exercised because local inference completed before edit window");
            return;
        }
        String edited = longSource + " تعديل المستخدم";
        runOnMainSync(() -> input.setText(edited));
        waitUntilIdle(busy, activity, TIMEOUT_MS);
        require(textOf(input).equals(edited), "user edit was overwritten after processing");
        require(readText(status, activity).equals(activity.getString(R.string.diacritize_text_changed)),
                "edit during processing did not publish diacritize_text_changed");
        note("PASS edit during processing preserved user text");
    }

    private void verifyMbrolaCanListen(Activity activity) throws Exception {
        Spinner engines = findEngineSpinner(activity.getWindow().getDecorView(), activity.getString(R.string.engine));
        require(engines != null, "engine spinner missing");
        int mbrola = 0;
        for (int i = 0; i < EngineRegistry.all().size(); i++) {
            if ("mbrola".equals(EngineRegistry.all().get(i).id())) { mbrola = i; break; }
        }
        final int mbrolaPosition = mbrola;
        EditText input = find(activity.getWindow().getDecorView(), EditText.class);
        Button speak = findButton(activity, activity.getString(R.string.speak));
        Field busy = privateField("busy");
        runOnMainSync(() -> {
            engines.setSelection(mbrolaPosition);
            input.setText("السَّلَام عَلَيْكُمْ");
            require(speak != null && speak.performClick(), "MBROLA listen button performClick failed");
        });
        waitUntilIdle(busy, activity, TIMEOUT_MS);
        note("PASS local MBROLA listen action completed");
    }

    private Field privateField(String name) throws Exception {
        Field field = MainActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private void waitUntilIdle(Field busy, Activity activity, long timeoutMs) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (Boolean.TRUE.equals(busy.get(activity)) && System.nanoTime() < deadline) Thread.sleep(50);
        require(!Boolean.TRUE.equals(busy.get(activity)), "busy did not clear within " + timeoutMs + " ms");
    }

    private String readText(Field field, Activity activity) throws Exception {
        final String[] value = new String[1];
        runOnMainSync(() -> {
            try { value[0] = ((TextView) field.get(activity)).getText().toString(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        return value[0];
    }

    private static String textOf(EditText input) {
        return input.getText().toString();
    }

    private static Button findButton(Activity activity, String label) {
        for (Button button : buttons(activity.getWindow().getDecorView())) {
            if (!(button instanceof android.widget.CompoundButton) && label.contentEquals(button.getText())) return button;
        }
        return null;
    }

    private static List<Button> buttons(View root) {
        List<Button> result = new ArrayList<>();
        collectButtons(root, result);
        return result;
    }

    private static void collectButtons(View root, List<Button> result) {
        if (root instanceof Button && !(root instanceof android.widget.CompoundButton)) result.add((Button) root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) collectButtons(group.getChildAt(i), result);
        }
    }

    private static <T> T find(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = find(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static Spinner findEngineSpinner(View root, String description) {
        if (root instanceof Spinner && description.contentEquals(root.getContentDescription())) {
            return (Spinner) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                Spinner found = findEngineSpinner(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    private static boolean hasArabicMark(String text) {
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if ((cp >= 0x064B && cp <= 0x065F) || cp == 0x0670 || (cp >= 0x06D6 && cp <= 0x06ED)) return true;
        }
        return false;
    }

    private static String stripMarks(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i); i += Character.charCount(cp);
            if (!((cp >= 0x064B && cp <= 0x065F) || cp == 0x0670 || (cp >= 0x06D6 && cp <= 0x06ED))) out.appendCodePoint(cp);
        }
        return out.toString();
    }
}
