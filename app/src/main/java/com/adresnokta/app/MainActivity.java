package com.adresnokta.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    EditText input;
    LinearLayout results;
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40,55,40,40);
        scroll.addView(root);

        TextView title=new TextView(this); title.setText("Adres Nokta"); title.setTextSize(30); title.setTypeface(null,Typeface.BOLD);
        root.addView(title);
        TextView sub=new TextView(this); sub.setText("Türkiye'deki açık adresi bulun ve Google Maps ile navigasyonu başlatın."); sub.setTextSize(16); sub.setPadding(0,12,0,28); root.addView(sub);

        input=new EditText(this); input.setHint("İl / İlçe / Mahalle / Sokak / No"); input.setMinLines(3); input.setGravity(Gravity.TOP); root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        Button find=new Button(this); find.setText("ADRESİ BUL"); root.addView(find,new LinearLayout.LayoutParams(-1,-2));
        status=new TextView(this); status.setPadding(0,20,0,10); status.setTextSize(15); root.addView(status);
        results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); root.addView(results);
        TextView attribution=new TextView(this); attribution.setText("Adres verisi © OpenStreetMap katkıda bulunanlar • Nominatim"); attribution.setTextSize(12); attribution.setPadding(0,30,0,20); root.addView(attribution);
        setContentView(scroll);
        find.setOnClickListener(v->search());
    }

    void search() {
        String q=input.getText().toString().trim();
        if(q.length()<5){ Toast.makeText(this,"Lütfen daha ayrıntılı bir adres girin.",Toast.LENGTH_SHORT).show(); return; }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
        results.removeAllViews(); status.setText("Adres aranıyor…");
        new Thread(()->{
            try{
                String url="https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=tr&addressdetails=1&limit=5&q="+URLEncoder.encode(q,"UTF-8");
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setRequestProperty("User-Agent","AdresNokta/1.0 (Android)");
                c.setConnectTimeout(12000); c.setReadTimeout(12000);
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder s=new StringBuilder(); String line; while((line=r.readLine())!=null)s.append(line); r.close();
                JSONArray a=new JSONArray(s.toString());
                runOnUiThread(()->showResults(a));
            }catch(Exception e){ runOnUiThread(()->status.setText("Adres sorgulanamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")); }
        }).start();
    }

    void showResults(JSONArray a){
        results.removeAllViews();
        if(a.length()==0){status.setText("Eşleşen adres bulunamadı. İl, ilçe, mahalle, sokak ve kapı numarasını yazarak tekrar deneyin.");return;}
        status.setText(a.length()==1?"1 sonuç bulundu.":"Birden fazla sonuç bulundu. Doğru adresi seçin:");
        for(int i=0;i<a.length();i++) try{
            JSONObject o=a.getJSONObject(i);
            String name=o.optString("display_name");
            double lat=o.getDouble("lat"), lon=o.getDouble("lon");
            JSONObject ad=o.optJSONObject("address");
            boolean building=ad!=null && ad.has("house_number");
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(24,22,24,22);
            TextView t=new TextView(this); t.setText(name); t.setTextSize(16); t.setTypeface(null,Typeface.BOLD); card.addView(t);
            TextView p=new TextView(this); p.setText((building?"✓ Kapı/bina numarası bulundu":"⚠ Bina düzeyinde kesin sonuç bulunamadı")+"\nKonum: "+lat+", "+lon); p.setPadding(0,10,0,10); card.addView(p);
            Button go=new Button(this); go.setText("GOOGLE MAPS İLE GİT"); go.setOnClickListener(v->navigate(lat,lon)); card.addView(go);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,8,0,18); results.addView(card,cp);
        }catch(Exception ignored){}
    }

    void navigate(double lat,double lon){
        Uri nav=Uri.parse("google.navigation:q="+lat+","+lon+"&mode=d");
        Intent i=new Intent(Intent.ACTION_VIEW,nav); i.setPackage("com.google.android.apps.maps");
        try{startActivity(i);}catch(Exception e){
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+lat+","+lon)));
        }
    }
}
