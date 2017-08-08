from python_speech_features import  mfcc
from scipy.signal import hamming
from os import listdir
from numpy import savetxt as svtxt
import scipy.io.wavfile as wav

def get_features(filepath):
    (rate, signal) = wav.read(filepath)
    mfcc_features = mfcc(signal=signal, samplerate=rate, winlen=0.025, winstep=0.01, winfunc=lambda m: hamming(m), appendEnergy=False)
    return mfcc_features

def write_speaker_features(speaker_dir_path):
    for file in listdir(speaker_dir_path):
        if file.endswith("wav"):
            featureFilePath = speaker_dir_path + '/' + file.replace('wav', 'ftr')
            svtxt(featureFilePath, get_features(speaker_dir_path + '/' + file), fmt='%.6f')

def write_all_speakers_features(database_path):
    for file in listdir(database_path):
        write_speaker_features(database_path + file)
