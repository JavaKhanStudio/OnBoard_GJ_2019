package jks.sounds;

import static jks.sounds.GVars_Audio.masterVolume;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import jks.debug.GVars_Debug;

public class GVars_AudioManager 
{	
	private static ArrayList<Sound> jumpingSounds;

	private static Sound runningSound;


	private static FileHandle musicFile = Gdx.files.internal("musics/intro.mp3");

	private static Music currentlyRunningMusic;
	private static Music currentlyRunningMusicSecondary;
	private static Music currentlyRunningAmbiance;

	public static void init() 
	{
		
	}

	private static void PreLoadAllSounds()
	{
//		jumpingSounds = new ArrayList<>();
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_01.wav")));
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_02.wav")));
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_03.wav")));
//
//		runningSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Run_Loop.wav"));

	}

	public static void PlayGameSound(Enum_Sounds_Game whichOne) 
	{
		if(GVars_Audio.muted)
			return ;
		
		if(GVars_Debug.soundDebug)
			System.out.println("Trying to play sound : " + whichOne);
		
		switch (whichOne) 
		{
			case Jumping:
				int randomizedIndex = MathUtils.random.nextInt(jumpingSounds.size());
				runningSound.stop();
				jumpingSounds.get(randomizedIndex).play(masterVolume);
				break;
	
			case Running:
				runningSound.stop();
				runningSound.loop(masterVolume, 1.1f, 0) ;
				break;
	
			case Idlling:
				runningSound.stop();
				break;
						
			default:
				System.out.println("Unknown Sound requested in PlaySound : " + whichOne);
				break;
		}
	}
	
	public static void PlayInterfaceSound(Enum_Sounds_Game whichOne) 
	{
		if(GVars_Audio.muted)
			return ;
		
		if(GVars_Debug.soundDebug)
			System.out.println("Trying to play sound : " + whichOne);
		
		switch (whichOne) 
		{
			case Jumping:
				int randomizedIndex = MathUtils.random.nextInt(jumpingSounds.size());
				runningSound.stop();
				jumpingSounds.get(randomizedIndex).play(masterVolume);
				break;
	
			case Running:
				runningSound.stop();
				runningSound.loop(masterVolume, 1.1f, 0) ;
				break;
	
			case Idlling:
				runningSound.stop();
				break;
						
			default:
				System.out.println("Unknown Sound requested in PlaySound : " + whichOne);
				break;
		}
	}

	public static void PlayMusic(Enum_Music whichOne) 
	{
		if(GVars_Audio.muted)
			return ;

		if(GVars_Debug.soundDebug)
			System.out.println("Trying to play Music : " + whichOne);
		
		switch (whichOne) 
		{
			case STARTING_SCREEN:
				if (currentlyRunningMusic == null) 
				{
					currentlyRunningMusic = Gdx.audio.newMusic(musicFile);
					currentlyRunningMusic.setLooping(false);
					currentlyRunningMusic.play();
				}
				break;
			case GAME_INTRO:
				if (currentlyRunningMusic == null) 
				{
					currentlyRunningMusic = Gdx.audio.newMusic(musicFile);
					currentlyRunningMusic.setLooping(false);
					currentlyRunningMusic.play();
				}
				break;		
			default:
				System.out.println("Unknown Music requested in PlayMusic : " + whichOne);
				break;
		}
	}

	public static void StopAndDisposeMusic() 
	{
		if(currentlyRunningMusic != null)
		{
			currentlyRunningMusic.stop() ;
			currentlyRunningMusic.dispose() ;
			currentlyRunningMusic = null ;
		}
		if(currentlyRunningMusicSecondary != null)
		{
			currentlyRunningMusicSecondary.stop() ;
			currentlyRunningMusicSecondary.dispose() ;
			currentlyRunningMusicSecondary = null ;
		}
		if(currentlyRunningAmbiance != null)
		{
			currentlyRunningAmbiance.stop() ;
			currentlyRunningAmbiance.dispose() ;
			currentlyRunningAmbiance = null ;
		}
	}
}
