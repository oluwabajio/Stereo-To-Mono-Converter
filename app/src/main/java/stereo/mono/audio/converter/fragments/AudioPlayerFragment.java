package stereo.mono.audio.converter.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.roger.catloadinglibrary.CatLoadingView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import stereo.mono.audio.converter.R;
import stereo.mono.audio.converter.databinding.FragmentAudioPlayerBinding;
import stereo.mono.audio.converter.utils.CustomProgress;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class AudioPlayerFragment extends Fragment implements Runnable{

    private static final int PERMISSION_REQUEST_CODE = 107;
    FragmentAudioPlayerBinding binding;
    private static final String TAG = "AudioPlayerFragment";
    MediaPlayer mediaPlayer2;
    MediaPlayer mediaPlayer = new MediaPlayer();
    boolean wasPlaying = false;
    private InterstitialAd mInterstitialAd;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAudioPlayerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

       // initMediaPlayer();
        initListeners();
        if (checkPermission()) {
            getMediaInfo();
            convertToMono(getSelectedAudioPath());
            playSong(getSelectedAudioPath());
        }
        initAds();
        return view;
    }

    private void initAds() {
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId("ca-app-pub-9562015878942760/3248290340");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void initListeners() {
        String songName = getSelectedAudioPath().substring(getSelectedAudioPath().lastIndexOf("/")+1);
        binding.tvSongName.setText(songName);

        binding.fab.setOnClickListener(v-> {
            playPauseMusic();
        });

        binding.checkboxMono.setOnCheckedChangeListener((buttonView, isChecked) -> {
            playAudioInMonoOrStereo(isChecked);
        });

        binding.btnLeftOnly.setOnClickListener(v-> {
            convertToLeftOnly(getSelectedAudioPath());
        });

        binding.btnRightOnly.setOnClickListener(v-> {
            convertToRightOnly(getSelectedAudioPath());
        });

        binding.seekBarPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                binding.seekBarHint.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                binding.seekBarHint.setVisibility(View.VISIBLE);
                int x = (int) Math.ceil(progress / 1000f);

                if (x < 10)
                    binding.seekBarHint.setText("0:0" + x);
                else
                    binding.seekBarHint.setText("0:" + x);

                double percent = progress / (double) seekBar.getMax();
                int offset = seekBar.getThumbOffset();
                int seekWidth = seekBar.getWidth();
                int val = (int) Math.round(percent * (seekWidth - 2 * offset));
                int labelWidth = binding.seekBarHint.getWidth();
                binding.seekBarHint.setX(offset + seekBar.getX() + val
                        - Math.round(percent * offset)
                        - Math.round(percent * labelWidth / 2));

                if (progress > 0 && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    //clearMediaPlayer();
                    binding.fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
                 //   binding.seekBarPosition.setProgress(0);
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });

        binding.btnconvertStereoToMono.setOnClickListener(v-> {
        showDownloadCompleteDialog(getMonoAudioPath());
        });


    }

    private void showDownloadCompleteDialog(String path) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Stereo TO Mono Converter")
                .setMessage("Audio FIle Converted Successfully and saved at "+ path)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        dialog.dismiss();
                        showInterstitialAd();
                    }
                })

                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void showInterstitialAd() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.e("TAG", "The interstitial wasn't loaded yet.");
        }
    }

    private void playAudioInMonoOrStereo(boolean isChecked) {
        if (isChecked) {
            playSong(getMonoAudioPath());
        } else {
            playSong(getSelectedAudioPath());
        }
    }

    private void playPauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            binding.fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            binding.fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_pause));
            new Thread(this).start();
        }
    }

    private void playSong(String audioPath) {

        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                clearMediaPlayer();
                binding.seekBarPosition.setProgress(0);
                binding.fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_play));
            }

                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }

                binding.fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_media_pause));


                mediaPlayer.setDataSource(getActivity(), Uri.parse(audioPath));

                mediaPlayer.prepare();
                mediaPlayer.setVolume(0.5f, 0.5f);
                mediaPlayer.setLooping(false);
                binding.seekBarPosition.setMax(mediaPlayer.getDuration());

                mediaPlayer.start();
                new Thread(this).start();


        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void run() {

        int currentPosition = mediaPlayer.getCurrentPosition();
        int total = mediaPlayer.getDuration();


        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }

            binding.seekBarPosition.setProgress(currentPosition);

        }
    }



    private void clearMediaPlayer() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void getMediaInfo() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(getSelectedAudioPath());// the adresss location of the sound on sdcard.
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, "getMediaInfo: Error " + e.getMessage());
        }

        MediaFormat mediaFormat = mediaExtractor.getTrackFormat(0);

        int bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        Log.e(TAG, "getMediaInfo: "+ getSelectedAudioPath() );
        binding.tvFormat.setText(getSelectedAudioPath().substring(getSelectedAudioPath().lastIndexOf("."), getSelectedAudioPath().length()));
        binding.tvSamplingRate.setText("" + sampleRate);
        binding.tvBitrate.setText("" + bitRate);
        Log.e(TAG, "getMediaInfo: Channel count is " + channelCount);
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getSelectedAudioPath());//Write your location here
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSelectedAudioPath() {
        return getArguments().getString("url");
    }
    private String getMonoAudioPath() {
        return getArguments().getString("mono_url");
    }

    private void convertToMono(String originalPath) {
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory("") + "/StereoToMono/");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputDir = outputDirectory.getAbsolutePath();
        String ffmpegString = getFFMPEGString(originalPath, outputDir);
        Log.e(TAG, "convertToMono: " + originalPath);

        long executionId = FFmpeg.executeAsync(ffmpegString, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.e(Config.TAG, "Async command execution completed successfully.");
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.e(Config.TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                }
            }
        });
    }


    private void convertToLeftOnly(String originalPath) {
        CatLoadingView mView  = new CatLoadingView();
        mView.show(getFragmentManager(), "");
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory("") + "/StereoToMono/");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputDir = outputDirectory.getAbsolutePath();
        String outputPath = outputDir + "/StereoToMono_left_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + ".mp3";
        String ffmpegString = getMonoLeftOnlyFFMPEGString(originalPath, outputPath);
        Log.e(TAG, "convertToMono: " + originalPath);

        long executionId = FFmpeg.executeAsync(ffmpegString, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                mView.dismiss();
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.e(Config.TAG, "Async command execution completed successfully.");
                    showDownloadCompleteDialog(outputPath);
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.e(Config.TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                    Toast.makeText(getActivity(), "Error: Please Try Again!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void convertToRightOnly(String originalPath) {
        CatLoadingView mView  = new CatLoadingView();
        mView.show(getFragmentManager(), "");
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory("") + "/StereoToMono/");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String outputDir = outputDirectory.getAbsolutePath();
        String outputPath = outputDir + "/StereoToMono_right_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + ".mp3";
        String ffmpegString = getMonoRightOnlyFFMPEGString(originalPath, outputPath);

        Log.e(TAG, "convertToMono: " + originalPath);

        long executionId = FFmpeg.executeAsync(ffmpegString, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                mView.dismiss();
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Log.e(Config.TAG, "Async command execution completed successfully.");
                    showDownloadCompleteDialog(outputPath);
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.e(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.e(Config.TAG, String.format("Async command execution failed with rc=%d.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                    Toast.makeText(getActivity(), "Error: Please Try Again!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private String getFFMPEGString(String originalPath, String outputDir) {
        return String.format("-i  '%s' -ac 1 '%s'", originalPath, outputDir + "/StereoToMono_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()) + ".mp3");
    }

    private String getMonoLeftOnlyFFMPEGString(String originalPath, String outputPath) {
        return String.format("-i  '%s' -filter_complex \"[0:a]channelsplit=channel_layout=stereo:channels=FL[left]\" -map \"[left]\" '%s'", originalPath, outputPath);
    }

    private String getMonoRightOnlyFFMPEGString(String originalPath, String outputPath) {
        return String.format("-i  '%s' -filter_complex \"[0:a]channelsplit=channel_layout=stereo:channels=FR[right]\" -map \"[right]\" '%s'", originalPath, outputPath);
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
    @Override
    public void onDestroy() {
        super.onDestroy();
        clearMediaPlayer();
    }

}