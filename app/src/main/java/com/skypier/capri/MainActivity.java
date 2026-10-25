package com.skypier.capri;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private TextView output, status;
    private EditText input;
    private TextToSpeech tts;
    private final int SPEECH_REQUEST = 42;
    private final int bg = Color.rgb(12, 15, 27), accent = Color.rgb(104, 229, 228);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS && tts != null)
                tts.setLanguage(new Locale("pt", "BR"));
        });
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 45, 28, 35);
        scroll.addView(layout);
        setContentView(scroll);
        TextView title = text("C A P R I", 32, accent);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(title);
        layout.addView(text("CONTROL 1.0  •  ANDROID", 13, Color.LTGRAY));
        status = text("", 14, accent);
        layout.addView(status);
        addButton(layout, "ATIVAR CONTROLE DO CELULAR",
            () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        output = text("CAPRI: Pronta. Ative o serviço de acessibilidade para eu controlar a tela quando você pedir.", 17, Color.WHITE);
        output.setPadding(0, 30, 0, 25);
        layout.addView(output);
        input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("Ex.: abre YouTube, volta, rola para baixo");
        input.setSingleLine(false);
        layout.addView(input);
        addButton(layout, "EXECUTAR COMANDO", () -> execute(input.getText().toString()));
        addButton(layout, "FALAR COM A CAPRI", this::listen);
        addButton(layout, "VOLTAR", () -> execute("voltar"));
        addButton(layout, "TELA INICIAL", () -> execute("inicio"));
        addButton(layout, "ROLAR PARA BAIXO", () -> execute("rolar para baixo"));
        addButton(layout, "ROLAR PARA CIMA", () -> execute("rolar para cima"));
        layout.addView(text("Comandos: abrir [app], voltar, início, notificações, recentes, rolar para cima/baixo, tocar em [texto]. Sem escuta contínua. Ações apenas quando solicitadas.", 13, Color.LTGRAY));
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) status.setText(CapriAccessibilityService.instance == null ?
            "● Controle desativado: habilite nas configurações" : "● Controle de acessibilidade ativo");
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 9, 0, 9);
        return t;
    }

    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(bg);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 7, 0, 7);
        parent.addView(b, p);
        b.setOnClickListener(v -> action.run());
    }

    private String norm(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").trim();
    }

    private void say(String s) {
        output.setText("CAPRI: " + s);
        if (tts != null) tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "capri");
    }

    private void listen() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale um comando para a CAPRI");
            startActivityForResult(i, SPEECH_REQUEST);
        } catch (Exception e) {
            say("Não encontrei reconhecimento de voz disponível neste celular.");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SPEECH_REQUEST && result == RESULT_OK && data != null) {
            ArrayList<String> a = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (a != null && !a.isEmpty()) {
                input.setText(a.get(0));
                execute(a.get(0));
            }
        }
    }

    private void execute(String raw) {
        String s = norm(raw);
        CapriAccessibilityService service = CapriAccessibilityService.instance;
        if (s.isEmpty()) { say("Escreva ou fale um comando."); return; }
        if (s.startsWith("abrir ") || s.startsWith("abre ")) {
            String app = s.replaceFirst("^(abrir|abre)\\s+", "");
            openApp(app);
            return;
        }
        if (service == null) {
            say("Primeiro ative a CAPRI em Acessibilidade nas configurações do Android.");
            return;
        }
        if (s.equals("voltar") || s.equals("volta")) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            say("Voltando.");
        } else if (s.equals("inicio") || s.contains("tela inicial")) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            say("Tela inicial.");
        } else if (s.contains("notificac")) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            say("Notificações abertas.");
        } else if (s.contains("recentes")) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS);
            say("Aplicativos recentes.");
        } else if (s.contains("rolar") || s.contains("deslizar")) {
            boolean down = !(s.contains("cima") || s.contains("subir"));
            say(service.scroll(down) ? "Rolagem solicitada." : "Não consegui rolar esta tela.");
        } else if (s.startsWith("tocar em ") || s.startsWith("clica em ") || s.startsWith("clicar em ")) {
            String target = s.replaceFirst("^(tocar em|clica em|clicar em)\\s+", "");
            say(service.tapText(target) ? "Toque solicitado em " + target + "." :
                "Não encontrei esse elemento na tela: " + target);
        } else {
            say("Ainda não conheço esse comando. Experimente abrir YouTube, voltar ou rolar para baixo.");
        }
    }

    private void openApp(String name) {
        Map<String, String> known = new HashMap<>();
        known.put("youtube", "com.google.android.youtube");
        known.put("youtube music", "com.google.android.apps.youtube.music");
        known.put("spotify", "com.spotify.music");
        known.put("whatsapp", "com.whatsapp");
        known.put("chrome", "com.android.chrome");
        known.put("instagram", "com.instagram.android");
        known.put("configuracoes", "com.android.settings");
        known.put("camera", "com.sec.android.app.camera");
        String pkg = known.get(name);
        if (pkg == null) {
            say("Ainda não tenho esse aplicativo no catálogo de comandos: " + name);
            return;
        }
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) { say("Não encontrei " + name + " instalado ou acessível."); return; }
            startActivity(i);
            say("Abrindo " + name + ".");
        } catch (Exception e) {
            say("Não consegui abrir " + name + ".");
        }
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
