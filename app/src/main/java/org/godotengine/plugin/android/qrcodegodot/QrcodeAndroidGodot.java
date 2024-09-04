package org.godotengine.plugin.android.qrcodegodot;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.Objects;
import java.util.Set;

public class QrcodeAndroidGodot extends GodotPlugin {
    private final SignalInfo qrcodeReadSignal = new SignalInfo("onQrcodeRead", String.class);
    private final GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build();
    private ModuleInstallClient moduleInstallClient;
    private GmsBarcodeScanner scanner;

    /**
     * Base constructor passing a {@link Godot} instance through which the plugin can access Godot's
     * APIs and lifecycle events.
     *
     * @param godot
     */
    public QrcodeAndroidGodot(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return BuildConfig.GODOT_PLUGIN_NAME;
    }

    @Override
    public void onGodotSetupCompleted() {
        super.onGodotSetupCompleted();
        scanner = GmsBarcodeScanning.getClient(Objects.requireNonNull(getActivity()), options);
        moduleInstallClient = ModuleInstall.getClient(Objects.requireNonNull(getActivity()));
        final var moduleInstallRequest = ModuleInstallRequest.newBuilder().addApi(scanner).build();
        moduleInstallClient.installModules(moduleInstallRequest);
    }

    @UsedByGodot
    public void scanQrcode() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            moduleInstallClient.areModulesAvailable(scanner).addOnSuccessListener(command -> {
                        if (command.areModulesAvailable()) {
                            startQrcodeScan();
                        } else {
                            installModule();
                        }
                    }
            );
        });
    }

    private void installModule() {
        var moduleInstallRequest = ModuleInstallRequest.newBuilder().addApi(scanner).build();
        moduleInstallClient.installModules(moduleInstallRequest).addOnSuccessListener(command -> {
            if (command.areModulesAlreadyInstalled()) {
                startQrcodeScan();
            } else {
                Toast.makeText(getActivity(), "Installing Module", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(command -> {
            Toast.makeText(getActivity(), "Fail install module", Toast.LENGTH_LONG).show();
            moduleInstallClient.deferredInstall(scanner);
        });
    }

    private void startQrcodeScan() {
        scanner.startScan()
                .addOnSuccessListener(barcode -> emitSignal(qrcodeReadSignal.getName(), barcode.getRawValue()))
                .addOnCanceledListener(() -> Toast.makeText(getActivity(), "Canceled", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        return Set.of(qrcodeReadSignal);
    }
}
