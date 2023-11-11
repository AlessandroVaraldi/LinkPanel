package com.example.linkpanel;

import android.text.InputType;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.MenuItem;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private int numButtons = 1; // Tracks the number of buttons dynamically
    private LinearLayout buttonContainer;
    private SharedPreferences prefs;
    public int maxStringLength = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        buttonContainer = findViewById(R.id.buttonContainer);

        // Crea dinamicamente i pulsanti
        numButtons = prefs.getInt("num_buttons", 1);
        restoreButtons(numButtons);
    }

    private void restoreButtons(int numButtons) {
        // Restore custom buttons on startup
        for (int i = 0; i < numButtons; i++) {
            String buttonTextKey = "button_text_" + i;
            String buttonUrlKey = "button_url_" + i;
            String buttonText = prefs.getString(buttonTextKey, "Pulsante " + (i + 1));
            String buttonUrl = prefs.getString(buttonUrlKey, "http://example.com");
            addButtonUI(buttonText, buttonUrl, i);
        }
    }

    private void createButton() {
        // Create a button...
        String buttonText = getString(R.string.button) + (numButtons + 1);
        String buttonUrl = getString(R.string.exampleDomain);
        addButtonUI(buttonText, buttonUrl, numButtons);
        numButtons++;
    }

    private void addButtonUI(String buttonText, String buttonUrl, int index) {
        // Add the created button to the UI...
        Button button = configureButton(buttonText);
        button.setTag(index);

        button.setOnClickListener(v -> openWebPage(buttonUrl));
        button.setOnLongClickListener(v -> {
            showPopupMenu(v, index);
            return true;
        });

        buttonContainer.addView(button);

        // Save data and count after adding a button
        saveButtonData(index, true, buttonText);
        saveButtonData(index, false, buttonUrl);
        saveButtonCount();
    }

    private Button configureButton(String buttonText) {
        // Configuration of button appearance...
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setBackgroundTintList(ColorStateList.valueOf(0xFF1E4918));
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setPadding(12, 12, 12, 12);
        button.setText(buttonText);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 20);
        button.setLayoutParams(layoutParams);

        return button;
    }

    private void openWebPage(String url) {
        // Opening a web page...
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        Intent chooser = Intent.createChooser(intent, getString(R.string.openWith));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, R.string.noAppAvailable, Toast.LENGTH_LONG).show();
        }
    }

    private void showPopupMenu(View view, int buttonIndex) {
        // Popup menu for editing/deleting a button...
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_str){
                showEditDialog(buttonIndex, true);
                return true;
            } else if (item.getItemId() == R.id.action_edit_url){
                showEditDialog(buttonIndex, false);
                return true;
            } else if (item.getItemId() == R.id.delete_button){
                if (numButtons == 1){
                    Toast.makeText(this, R.string.noZeroButtons, Toast.LENGTH_LONG).show();
                    clearAllButtons();
                } else deleteButton(buttonIndex);
                return true;
            } else if (item.getItemId() == R.id.move_up){
                moveButton(buttonIndex, true);
                return true;
            } else if (item.getItemId() == R.id.move_down){
                moveButton(buttonIndex, false);
                return true;
            } else return false;
        });
        popupMenu.show();
    }

    private void moveButton(int buttonIndex, boolean moveUp) {
        if (moveUp && buttonIndex == 0) {
            Toast.makeText(this, R.string.cannotMoveUp, Toast.LENGTH_SHORT).show();
            return;
        } else if (!moveUp && buttonIndex == numButtons - 1) {
            Toast.makeText(this, R.string.cannotMoveDown, Toast.LENGTH_SHORT).show();
            return;
        }

        int swapIndex = moveUp ? buttonIndex - 1 : buttonIndex + 1;

        // Swap in SharedPreferences
        swapButtonDataInPrefs(buttonIndex, swapIndex);

        // Update UI
        Button currentButton = findButtonByIndex(buttonIndex);
        Button swapButton = findButtonByIndex(swapIndex);

        if (currentButton != null && swapButton != null) {
            // Swap positions in the LinearLayout
            buttonContainer.removeView(currentButton);
            buttonContainer.removeView(swapButton);

            if (moveUp) {
                buttonContainer.addView(currentButton, swapIndex);
                buttonContainer.addView(swapButton, buttonIndex);
            } else {
                buttonContainer.addView(swapButton, buttonIndex);
                buttonContainer.addView(currentButton, swapIndex);
            }

            // Update tags
            currentButton.setTag(swapIndex);
            swapButton.setTag(buttonIndex);
        }

        // Update button click and long click listeners if necessary
        updateButtonListeners();
    }

    private void swapButtonDataInPrefs(int buttonIndex, int swapIndex) {
        SharedPreferences.Editor editor = prefs.edit();

        String currentButtonText = prefs.getString("button_text_" + buttonIndex, "");
        String currentButtonUrl = prefs.getString("button_url_" + buttonIndex, "");
        String swapButtonText = prefs.getString("button_text_" + swapIndex, "");
        String swapButtonUrl = prefs.getString("button_url_" + swapIndex, "");

        editor.putString("button_text_" + buttonIndex, swapButtonText);
        editor.putString("button_url_" + buttonIndex, swapButtonUrl);
        editor.putString("button_text_" + swapIndex, currentButtonText);
        editor.putString("button_url_" + swapIndex, currentButtonUrl);

        editor.apply();
    }

    private void updateButtonListeners() {
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            Button button = (Button) buttonContainer.getChildAt(i);
            int buttonIndex = (int) button.getTag();

            button.setOnClickListener(v -> openWebPage(prefs.getString("button_url_" + buttonIndex, "")));
            button.setOnLongClickListener(v -> {
                showPopupMenu(v, buttonIndex);
                return true;
            });
        }
    }

    private void deleteButton(int buttonIndex) {
        // Deleting a button...
        Button buttonToRemove = findButtonByIndex(buttonIndex);
        if (buttonToRemove != null) {
            buttonContainer.removeView(buttonToRemove);
        }

        numButtons--;

        SharedPreferences.Editor editor = prefs.edit();
        for (int i = buttonIndex; i < numButtons; i++) {
            String buttonText = prefs.getString("button_text_" + (i + 1), null);
            String buttonUrl = prefs.getString("button_url_" + (i + 1), null);
            editor.putString("button_text_" + i, buttonText);
            editor.putString("button_url_" + i, buttonUrl);
        }

        editor.remove("button_text_" + numButtons);
        editor.remove("button_url_" + numButtons);
        editor.apply();

        updateButtonsAfterDeletion();
        saveButtonCount();
    }

    private void updateButtonsAfterDeletion() {
        // Updating buttons after deletion...
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            Button button = (Button) buttonContainer.getChildAt(i);
            button.setTag(i);

            int finalI = i;
            button.setOnClickListener(v -> {
                String buttonUrl = prefs.getString("button_url_" + finalI, "");
                openWebPage(buttonUrl);
            });

            button.setOnLongClickListener(v -> {
                showPopupMenu(v, finalI);
                return true;
            });

            String buttonText = prefs.getString("button_text_" + i, "");
            button.setText(buttonText);
        }
        saveButtonCount();
    }

    private void showEditDialog(int buttonIndex, boolean isText) {
        // Showing edit dialog...
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isText ? getString(R.string.editText) : getString(R.string.editUrl));

        final EditText input = new EditText(this);
        input.setInputType(isText ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_TEXT_VARIATION_URI);
        String currentData = prefs.getString(isText ? "button_text_" + buttonIndex : "button_url_" + buttonIndex, "");
        input.setText(currentData);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String newData = input.getText().toString();

            if (isText && newData.length() > maxStringLength) {
                Toast.makeText(getApplicationContext(), R.string.stringTooLong, Toast.LENGTH_SHORT).show();
            } else if (!isText && !isValidUrl(newData)) {
                Toast.makeText(getApplicationContext(), R.string.invalidUrl, Toast.LENGTH_SHORT).show();
            } else {
                saveButtonData(buttonIndex, isText, newData);
                updateButton(buttonIndex, isText, newData);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private boolean isValidUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            parsedUrl.toURI();

            if (parsedUrl.getProtocol() == null ||
                    (!parsedUrl.getProtocol().equalsIgnoreCase("http") &&
                            !parsedUrl.getProtocol().equalsIgnoreCase("https"))) {
                return false;
            }

            if (parsedUrl.getHost() == null || parsedUrl.getHost().isEmpty()) {
                return false;
            }

            return url.matches(getString(R.string.allowedUrlCharacters));

        } catch (MalformedURLException | URISyntaxException e) {
            Log.e("MainActivity", "Invalid URL: " + e.getMessage());
            return false;
        }
    }

    private void saveButtonData(int buttonIndex, boolean isText, String data) {
        // Saving button data...
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(isText ? "button_text_" + buttonIndex : "button_url_" + buttonIndex, data);
        editor.apply();
    }

    private void updateButton(int buttonIndex, boolean isText, String newData) {
        // Updating a button...
        Button buttonToUpdate = findButtonByIndex(buttonIndex);

        if (buttonToUpdate != null) {
            if (isText) {
                buttonToUpdate.setText(newData);
                buttonToUpdate.invalidate();
            } else {
                buttonToUpdate.setOnClickListener(v -> openWebPage(newData));
            }
        }
    }

    private Button findButtonByIndex(int buttonIndex) {
        // Finding a button by index...
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            Button button = (Button) buttonContainer.getChildAt(i);
            if (button.getTag() != null && button.getTag() instanceof Integer && (int) button.getTag() == buttonIndex) {
                return button;
            }
        }
        return null;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Options menu...
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handling item selection...
        int id = item.getItemId();

        if (id == R.id.action_clear_all) {
            showClearConfirmationDialog();
        } else if (id == R.id.action_add_button) {
            if(numButtons < 25){
                createButton();
            } else {
                Toast.makeText(getApplicationContext(), R.string.maxButtonsAlert, Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clearTitle)
                .setMessage(R.string.clearQuestion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> clearAllButtons())
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void clearAllButtons() {
        // Clearing all buttons..
        buttonContainer.removeAllViews();

        numButtons = 0;
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putInt("num_buttons", numButtons);
        editor.apply();
        createButton(); // Create a button so UI is not empty
    }

    private void saveButtonCount() {
        // Saves the current button count to SharedPreferences...
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("num_buttons", numButtons);
        editor.apply();
    }
}