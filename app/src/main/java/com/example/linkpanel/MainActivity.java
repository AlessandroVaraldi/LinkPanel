package com.example.linkpanel;

import android.text.InputType;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.Button;
import android.content.Intent;
import android.net.Uri;
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
    private static int NUM_BUTTONS = 1; // Numero dinamico di pulsanti
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
        NUM_BUTTONS = prefs.getInt("num_buttons", 1);
        createButtons(NUM_BUTTONS);
    }

    private void addButton() {
        final int buttonIndex = NUM_BUTTONS;

        String buttonText = "Pulsante " + (buttonIndex + 1);
        String buttonUrl = "http://example.com";

        // Salva i valori di default nelle SharedPreferences prima di aggiungere il pulsante alla UI
        saveButtonData(buttonIndex, true, buttonText);
        saveButtonData(buttonIndex, false, buttonUrl);

        Button newButton = configureButton(buttonText);
        newButton.setTag(buttonIndex);

        newButton.setOnClickListener(v -> openWebPage(buttonUrl));

        // Configura i listener specifici per il nuovo pulsante, se necessario.
        newButton.setOnLongClickListener(v -> {
            showPopupMenu(v, buttonIndex);
            return true;
        });

        // Aggiungi il nuovo pulsante al contenitore
        buttonContainer.addView(newButton);

        // Incrementa il conteggio dei pulsanti e aggiorna le SharedPreferences
        NUM_BUTTONS++;
        saveButtonCount();
    }

    private void saveButtonCount() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("num_buttons", NUM_BUTTONS);
        editor.apply();
    }

    private void createButtons(int numButtons) {
        for (int i = 0; i < numButtons; i++) {
            final int index = i;
            String buttonTextKey = "button_text_" + i;
            String buttonUrlKey = "button_url_" + i;
            String buttonText = prefs.getString(buttonTextKey, "Pulsante " + (i + 1));
            String buttonUrl = prefs.getString(buttonUrlKey, "http://example.com");

            Button button = configureButton(buttonText);
            button.setTag(i);

            button.setOnClickListener(v -> openWebPage(buttonUrl));
            button.setOnLongClickListener(v -> {
                showPopupMenu(v, index);
                return true;
            });

            buttonContainer.addView(button);
        }
    }

    private Button configureButton(String buttonText) {
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
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        Intent chooser = Intent.createChooser(intent, "Apri con");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, "Nessuna applicazione disponibile per aprire il link", Toast.LENGTH_LONG).show();
        }
    }

    private void showPopupMenu(View view, int buttonIndex) {
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
                if (NUM_BUTTONS == 1){
                    Toast.makeText(this, "Non puoi avere zero pulsanti!", Toast.LENGTH_LONG).show();
                    clearAllButtons();
                    addButton();
                } else deleteButton(buttonIndex);
                return true;
            }else return false;
        });
        popupMenu.show();
    }

    private void deleteButton(int buttonIndex) {
        // Trova e rimuovi il pulsante dalla UI
        Button buttonToRemove = findButtonByIndex(buttonIndex);
        if (buttonToRemove != null) {
            buttonContainer.removeView(buttonToRemove);
        }

        // Decrementa il conteggio dei pulsanti
        NUM_BUTTONS--;

        // Sposta in avanti le SharedPreferences dei pulsanti rimanenti
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = buttonIndex; i < NUM_BUTTONS; i++) {
            String buttonText = prefs.getString("button_text_" + (i + 1), null);
            String buttonUrl = prefs.getString("button_url_" + (i + 1), null);
            editor.putString("button_text_" + i, buttonText);
            editor.putString("button_url_" + i, buttonUrl);
        }

        // Rimuovi le SharedPreferences dell'ultimo pulsante che ora è superfluo
        editor.remove("button_text_" + NUM_BUTTONS);
        editor.remove("button_url_" + NUM_BUTTONS);

        // Applica i cambiamenti alle SharedPreferences
        editor.apply();

        // Aggiorna il tag e il listener di click dei pulsanti rimanenti
        updateButtonsAfterDeletion();

        // Salva il nuovo conteggio dei pulsanti
        saveButtonCount();
    }

    private void updateButtonsAfterDeletion() {
        // Aggiorna il tag e i listener di click di tutti i pulsanti
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            Button button = (Button) buttonContainer.getChildAt(i);
            button.setTag(i); // Riassegna gli indici

            // Aggiorna il listener di click con l'URL corretto dalle SharedPreferences
            int finalI = i;
            button.setOnClickListener(v -> {
                String buttonUrl = prefs.getString("button_url_" + finalI, "");
                openWebPage(buttonUrl);
            });

            button.setOnLongClickListener(v -> {
                showPopupMenu(v, finalI);
                return true;
            });

            // Opzionalmente, aggiorna il testo del pulsante se necessario
            String buttonText = prefs.getString("button_text_" + i, "");
            button.setText(buttonText);
        }
    }

    private void showEditDialog(int buttonIndex, boolean isText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isText ? "Modifica Testo" : "Modifica URL");

        final EditText input = new EditText(this);
        input.setInputType(isText ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_TEXT_VARIATION_URI);
        String currentData = prefs.getString(isText ? "button_text_" + buttonIndex : "button_url_" + buttonIndex, "");
        input.setText(currentData);
        builder.setView(input);

        builder.setPositiveButton("Salva", (dialog, which) -> {
            String newData = input.getText().toString();

            if (isText && newData.length() > maxStringLength) {
                Toast.makeText(getApplicationContext(), "La stringa è troppo lunga", Toast.LENGTH_SHORT).show();
            } else if (!isText && !isValidUrl(newData)) {
                Toast.makeText(getApplicationContext(), "L'URL inserito non è valido", Toast.LENGTH_SHORT).show();
            } else {
                saveButtonData(buttonIndex, isText, newData);
                updateButton(buttonIndex, isText, newData);
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

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
            if (parsedUrl.getHost() == null) {
                throw new MalformedURLException("L'URL non ha un host valido.");
            }

            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    private void saveButtonData(int buttonIndex, boolean isText, String data) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(isText ? "button_text_" + buttonIndex : "button_url_" + buttonIndex, data);
        editor.apply();
    }

    private void updateButton(int buttonIndex, boolean isText, String newData) {
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
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            Button button = (Button) buttonContainer.getChildAt(i);
            if (button.getTag() != null && button.getTag() instanceof Integer && (int) button.getTag() == buttonIndex) {
                return button;
            }
        }
        return null;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gestisci le voci del menu dell'ActionBar qui.
        int id = item.getItemId();

        if (id == R.id.action_clear_all) {
            showClearConfirmationDialog();
        } else if (id == R.id.action_add_button) {
            if(NUM_BUTTONS < 25){
                addButton();
            } else {
                Toast.makeText(getApplicationContext(), "Massimo numero di pulsanti raggiunto!", Toast.LENGTH_LONG).show();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Conferma Rimozione")
                .setMessage("Sei sicuro di voler rimuovere tutti i pulsanti?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> clearAllButtons())
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void clearAllButtons() {
        // Rimuove tutti i pulsanti dalla LinearLayout
        buttonContainer.removeAllViews();

        // Reimposta il conteggio dei pulsanti a 0 e cancella le SharedPreferences
        NUM_BUTTONS = 0;
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Questo cancella TUTTE le SharedPreferences. Usa con cautela!
        editor.putInt("num_buttons", NUM_BUTTONS); // Salva il nuovo conteggio dei pulsanti
        editor.apply();
        addButton();
    }
}