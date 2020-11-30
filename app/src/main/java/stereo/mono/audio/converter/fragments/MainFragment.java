package stereo.mono.audio.converter.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.kbeanie.multipicker.api.AudioPicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenAudio;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.navigation.Navigation;
import stereo.mono.audio.converter.R;
import stereo.mono.audio.converter.databinding.FragmentMainBinding;
import stereo.mono.audio.converter.utils.CustomProgress;

import static android.app.Activity.RESULT_OK;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class MainFragment extends Fragment {

    FragmentMainBinding binding;
    AudioPicker audioPicker;
    private static final int PERMISSION_REQUEST_CODE = 103;
    private static final String TAG = "MainFragment";
    CustomProgress customProgress = CustomProgress.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       binding = FragmentMainBinding.inflate(inflater, container, false);
       View view = binding.getRoot();

        checkPermission();
        initListeners();


       return view;
    }

    private void initListeners() {
        binding.btnSelectAudio.setOnClickListener(v-> {
            if (checkPermission()) {
                pickAudioFile();
            } else {

            }
        });
    }


    private void pickAudioFile() {
        audioPicker = new AudioPicker(this);
        audioPicker.setAudioPickerCallback(new AudioPickerCallback() {
            @Override
            public void onAudiosChosen(List<ChosenAudio> files) {
                customProgress.showProgress(getActivity(), "Loading", false);
                convertToMono(files.get(0).getOriginalPath());

                    }
            @Override
            public void onError(String message) {
                // Handle errors
            }
        });
        audioPicker.pickAudio();
    }




    private void selectAudioFile() {
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        // request permission if it has not been grunted.
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }


    private void convertToMono(String originalPath) {
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory("") + "/StereoToMono/");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputDir = outputDirectory.getAbsolutePath();
        String outputFile =  outputDir+"/StereoToMono_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + ".mp3";
        String ffmpegString= getFFMPEGString(originalPath, outputFile);
        Log.e(TAG, "convertToMono: "+ffmpegString );
        Log.e(TAG, "convertToMono: " + originalPath);

        long executionId = FFmpeg.executeAsync(ffmpegString, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.e(Config.TAG, "Async command execution completed successfully.");
                    Bundle bundle = new Bundle();
                    bundle.putString("url", originalPath);
                    bundle.putString("mono_url", outputFile);
                    Navigation.findNavController(getView()).navigate(R.id.action_mainFragment_to_audioPlayerFragment, bundle);
                        customProgress.hideProgress();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.e(Config.TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                }
            }
        });
    }
    private String getFFMPEGString(String originalPath,  String outputFile) {
        return String.format("-i  '%s' -ac 1 '%s'", originalPath, outputFile);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Picker.PICK_AUDIO && resultCode == RESULT_OK) {
            audioPicker.submit(data);
        }
    }
}