package studio.efs.braviahome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Polecenia wyglądu z ADB (do zdjęć i testów):
 *   adb shell am broadcast -n studio.efs.braviahome/.LookCommandReceiver --es layout arc --es bg bg_03
 *
 * W manifeście odbiornik wymaga uprawnienia android.permission.DUMP. Ma je powłoka ADB, a zwykła
 * aplikacja nie może go dostać — żadna zainstalowana aplikacja nie zmieni więc wyglądu launchera.
 */
public class LookCommandReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        Look look = Look.load(prefs);
        if (!look.applyCommand(intent)) return;
        look.save(prefs);
        // Licznik zmian z zewnątrz: otwarty ekran główny nasłuchuje go i od razu się przebudowuje.
        prefs.edit().putLong(MainActivity.PREF_LOOK_COMMAND, System.currentTimeMillis()).apply();
    }
}
