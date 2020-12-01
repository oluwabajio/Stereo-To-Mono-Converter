package stereo.mono.audio.converter.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
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
import android.widget.SeekBar;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.kbeanie.multipicker.api.AudioPicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.AudioPickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenAudio;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import stereo.mono.audio.converter.databinding.FragmentHomeBinding;

import static android.app.Activity.RESULT_OK;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class HomeFragment extends Fragment {

    private static final int PICKER_REQUEST_CODE = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;
   FragmentHomeBinding binding;
    AudioPicker audioPicker;
    private static final String TAG = "HomeFragment";
    private String selectedAudioPath = "";

    MediaPlayer mp;


    private final static int MAX_VOLUME = 100;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();


        initListeners();
        checkPermission();



        return view;
    }

    private void initListeners() {
        binding.btnOpenFile.setOnClickListener(v -> {
            if (checkPermission()) {
                pickAudioFile();
            } else {
                Toast.makeText(getActivity(), "Kindly Accept Permissions", Toast.LENGTH_SHORT).show();
            }
        });


        binding.btnLeft.setOnClickListener(v -> {
            mp.setVolume(1f, 0.01f);
        });

        binding.btnRight.setOnClickListener(v -> {
            mp.setVolume(0.01f, 1f);
        });

        binding.seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = (float)  (MAX_VOLUME - progress) / MAX_VOLUME;
                 float volume2 = (float) (1 - volume);
                mp.setVolume(volume, volume2);
                Log.e(TAG, "onProgressChanged: volume is "+ volume + " volume2 is  "+volume2 );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.btnConvertToMono.setOnClickListener(v->{
            if (selectedAudioPath != "") {
                convertToMono(selectedAudioPath);
            } else {
                Toast.makeText(getActivity(), "Kindly Select an Audio File First", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickAudioFile() {
        audioPicker = new AudioPicker(this);
        audioPicker.setAudioPickerCallback(new AudioPickerCallback() {
            @Override
            public void onAudiosChosen(List<ChosenAudio> files) {

           //     decodeMp3WithJlayer(files.get(0).getOriginalPath());
                selectedAudioPath = files.get(0).getOriginalPath();
                playSoundPlay(files.get(0).getOriginalPath());
            }

            @Override
            public void onError(String message) {
                // Handle errors
            }
        });

        audioPicker.pickAudio();
    }

    private void convertToMono(String originalPath) {


        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/tiff3/");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        String asd =  String.format("-i  '%s' -ac 1 '%s'", originalPath, outputDirectory.getAbsolutePath()+"/dgsf.mp3");
        Log.e(TAG, "convertToMono: "+originalPath );
     //   long executionId = FFmpeg.executeAsync("-i "+originalPath+" -ac 1 "+ outputDirectory.getAbsolutePath()+"/dsf.mp3", new ExecuteCallback() {
        long executionId = FFmpeg.executeAsync(asd, new ExecuteCallback() {

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

    private void playSoundPlay(String originalPath) {
       if (mp != null) {
           if (mp.isPlaying()) {
               mp.stop();
           }
       }

        mp = new MediaPlayer();
        try {
            mp.setDataSource(originalPath);//Write your location here
            mp.prepare();
            mp.start();
            binding.seekBarVolume.setProgress(50);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void decodeMp3WithJlayer(String originalPath) {

        Decoder decoder = new Decoder();

        File file = new File(originalPath);
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);


//        InputStream mp3Source = new URL("your network mp3 source")
//                .openConnection()
//                .getInputStream();
            Bitstream bitStream = new Bitstream(fileInputStream);
            final int sampleRate = 44100;
            final int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();

            final int READ_THRESHOLD = 2147483647;

            Header frame;
            int framesReaded = 0;
            while (true) {

                if (!((frame = bitStream.readFrame()) != null))
                    break;

                SampleBuffer sampleBuffer = null;
                try {
                    sampleBuffer = (SampleBuffer) decoder.decodeFrame(frame, bitStream);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                short[] pcmChunk = sampleBuffer.getBuffer();


                short[] monoSamples= new short[pcmChunk.length];

                //additional counter
                int k=0;


                for(int i=0; i< monoSamples.length;i+=2){
                    //skip the header andsuperpose the samples of the left and right channel

                  //  monoSamples[i]= (short) ((pcmChunk[i*2]+ pcmChunk[(i*2)+1])/2);
                     monoSamples[i]= (short) ((pcmChunk[i]+ pcmChunk[i+1])/2);
                    monoSamples[i+1]= (short)  ((pcmChunk[i]+ pcmChunk[i+1])/2);

                    k++;
                }
                audioTrack.write(monoSamples, 0,monoSamples.length);
//                ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
//                for (short s : pcmChunk) {
//                    outStream.write(s & 0xff);
//                    outStream.write((s >> 8 ) & 0xff);
//                }

//                int N = pcmChunk.length;
//                ByteBuffer byteBuf = ByteBuffer.allocate(N);
//                int i=0;
//                while (N > i) {
//                    byte b = (byte)(pcmChunk[i]/256);  /*convert to byte. */
//                    byteBuf.put(b);
//                    i++;
//                }
//  audioTrack.write(byteBuf.array(), 0,(int) (pcmChunk.length / 1.1));


 // audioTrack.write(pcmChunk, 0,(int) (pcmChunk.length / 1.1));

                bitStream.closeFrame();
            }



        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "decodeMp3WithJlayer: " + e.getMessage());
        } catch (BitstreamException e) {
            e.printStackTrace();
            Log.e(TAG, "decodeMp3WithJlayer: " + e.getMessage());
        }
    }

    private void decodeMp3WithJlayer2(String originalPath) {

        Decoder decoder = new Decoder();

        File file = new File(originalPath);
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);


//        InputStream mp3Source = new URL("your network mp3 source")
//                .openConnection()
//                .getInputStream();
            Bitstream bitStream = new Bitstream(fileInputStream);
            final int sampleRate = 44100;
            final int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();

            final int READ_THRESHOLD = 2147483647;

            Header frame;
            int framesReaded = 0;
            while (true) {

                if (!((frame = bitStream.readFrame()) != null))
                    break;

                SampleBuffer sampleBuffer = null;
                try {
                    sampleBuffer = (SampleBuffer) decoder.decodeFrame(frame, bitStream);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                short[] pcmChunk = sampleBuffer.getBuffer();




//                ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
//                for (short s : pcmChunk) {
//                    outStream.write(s & 0xff);
//                    outStream.write((s >> 8 ) & 0xff);
//                }

//                int N = pcmChunk.length;
//                ByteBuffer byteBuf = ByteBuffer.allocate(N);
//                int i=0;
//                while (N > i) {
//                    byte b = (byte)(pcmChunk[i]/256);  /*convert to byte. */
//                    byteBuf.put(b);
//                    i++;
//                }
//  audioTrack.write(byteBuf.array(), 0,(int) (pcmChunk.length / 1.1));


                // audioTrack.write(pcmChunk, 0,(int) (pcmChunk.length / 1.1));

                bitStream.closeFrame();
            }



        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "decodeMp3WithJlayer: " + e.getMessage());
        } catch (BitstreamException e) {
            e.printStackTrace();
            Log.e(TAG, "decodeMp3WithJlayer: " + e.getMessage());
        }
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Picker.PICK_AUDIO && resultCode == RESULT_OK) {
            audioPicker.submit(data);
        }
    }


}
