package com.android.launcher;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class MyLauncherSettings extends PreferenceActivity implements OnPreferenceChangeListener {
    
	private static final String ALMOSTNEXUS_PREFERENCES = "launcher.preferences.almostnexus";
    private boolean shouldRestart=false;
    private String mMsg;
    private Context mContext;
    
    private static final String PREF_BACKUP_FILENAME = "adw_settings.xml";
    private static final String CONFIG_BACKUP_FILENAME = "adw_launcher.db";
    private static final String NAMESPACE = "com.android.launcher";
    // Request codes for onResultActivity. That way we know the request donw when startActivityForResult was fired
    private static final int REQUEST_SWIPE_DOWN_APP_CHOOSER = 0;
    private static final int REQUEST_HOME_BINDING_APP_CHOOSER = 1;
    private static final int REQUEST_SWIPE_UP_APP_CHOOSER = 2;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		//TODO: ADW should i read stored values after addPreferencesFromResource?
        if (Build.VERSION.SDK_INT >= 8)
            mMsg = getString(R.string.pref_message_restart_froyo);
        else
            mMsg = getString(R.string.pref_message_restart_normal);
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(ALMOSTNEXUS_PREFERENCES);
        addPreferencesFromResource(R.xml.launcher_settings);
        CheckBoxPreference uiDots= (CheckBoxPreference) findPreference("uiDots");
        uiDots.setOnPreferenceChangeListener(this);
        if(uiDots.isChecked()){
            CheckBoxPreference uiAB2= (CheckBoxPreference) findPreference("uiAB2");
        	uiAB2.setEnabled(false);
        }
        DialogSeekBarPreference columnsDesktop= (DialogSeekBarPreference) findPreference("desktopColumns");
        columnsDesktop.setMin(3);
        DialogSeekBarPreference rowsDesktop= (DialogSeekBarPreference) findPreference("desktopRows");
        rowsDesktop.setMin(3);
        DialogSeekBarPreference desktopScreens= (DialogSeekBarPreference) findPreference("desktopScreens");
        desktopScreens.setMin(1);
        desktopScreens.setOnPreferenceChangeListener(this);
        DialogSeekBarPreference defaultScreen= (DialogSeekBarPreference) findPreference("defaultScreen");
        defaultScreen.setMin(1);
        defaultScreen.setMax(AlmostNexusSettingsHelper.getDesktopScreens(this)-1);
        DialogSeekBarPreference columnsPortrait= (DialogSeekBarPreference) findPreference("drawerColumnsPortrait");
        columnsPortrait.setMin(1);
        DialogSeekBarPreference rowsPortrait= (DialogSeekBarPreference) findPreference("drawerRowsPortrait");
        rowsPortrait.setMin(1);
        DialogSeekBarPreference columnsLandscape= (DialogSeekBarPreference) findPreference("drawerColumnsLandscape");
        columnsLandscape.setMin(1);
        DialogSeekBarPreference rowsLandscape= (DialogSeekBarPreference) findPreference("drawerRowsLandscape");
        rowsLandscape.setMin(1);
        DialogSeekBarPreference zoomSpeed= (DialogSeekBarPreference) findPreference("zoomSpeed");
        zoomSpeed.setMin(300);
        DialogSeekBarPreference uiScaleAB= (DialogSeekBarPreference) findPreference("uiScaleAB");
        uiScaleAB.setMin(1);
        // wjax. Listen for changes in those ListPreference as if their values are BINDING_APP, then an app shall be selected via startActivityForResult
        ListPreference swipedown_action = (ListPreference) findPreference("swipedownActions");
        swipedown_action.setOnPreferenceChangeListener(this);
        ListPreference swipeup_action = (ListPreference) findPreference("swipeupActions");
        swipeup_action.setOnPreferenceChangeListener(this);
        ListPreference homebutton_binding = (ListPreference) findPreference("homeBinding");
        homebutton_binding.setOnPreferenceChangeListener(this);
        
        mContext=this;
        
        Preference exportToXML = findPreference("xml_export");
        exportToXML.setOnPreferenceClickListener(new OnPreferenceClickListener() {        
			public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle(getResources().getString(R.string.title_dialog_xml));
                alertDialog.setMessage(getResources().getString(R.string.message_dialog_export));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new ExportPrefsTask().execute();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        
        Preference importFromXML = findPreference("xml_import");
        importFromXML.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle(getResources().getString(R.string.title_dialog_xml));
                alertDialog.setMessage(getResources().getString(R.string.message_dialog_import));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new ImportPrefsTask().execute();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    
                    }
                });
                alertDialog.show();
                return true;
            }
        });        

        Preference exportConfig = findPreference("db_export");
        exportConfig.setOnPreferenceClickListener(new OnPreferenceClickListener() {        
			public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle(getResources().getString(R.string.title_dialog_xml));
                alertDialog.setMessage(getResources().getString(R.string.message_dialog_export_config));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new ExportDatabaseTask().execute();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    
                    }
                });
                alertDialog.show();
                return true;
            }
        });
        
        Preference importConfig = findPreference("db_import");
        importConfig.setOnPreferenceClickListener(new OnPreferenceClickListener() {        
			public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle(getResources().getString(R.string.title_dialog_xml));
                alertDialog.setMessage(getResources().getString(R.string.message_dialog_import_config));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new ImportDatabaseTask().execute();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), 
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    
                    }
                });
                alertDialog.show();
                return true;
            }
        });    
    }
	@Override
	protected void onPause(){
		if(shouldRestart){
			if(Build.VERSION.SDK_INT<=7){
				Intent intent = new Intent(getApplicationContext(), Launcher.class);
	            PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(),0, intent, 0);
	
	            // We want the alarm to go off 30 seconds from now.
	            Calendar calendar = Calendar.getInstance();
	            calendar.setTimeInMillis(System.currentTimeMillis());
	            calendar.add(Calendar.SECOND, 1);
	
	            // Schedule the alarm!
	            AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
	            am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
	   			ActivityManager acm = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		        acm.restartPackage("com.android.launcher");
			}else{
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
		super.onPause();
	}
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("desktopScreens")) {
			DialogSeekBarPreference pref = (DialogSeekBarPreference) findPreference("defaultScreen");
			pref.setMax((Integer) newValue);
		}else if(preference.getKey().equals("uiDots")) {
			CheckBoxPreference ab2=(CheckBoxPreference) findPreference("uiAB2");
			android.util.Log.d("PREFERENCES","new value="+newValue);
			if(newValue.equals(true)){
				ab2.setChecked(false);
				ab2.setEnabled(false);
			}else{
				ab2.setEnabled(true);
			}
		}else if(preference.getKey().equals("swipedownActions"))
		{
			// lets launch app picker if the user selected to launch an app on gesture
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER)))
			{
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	
	            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
	            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
	            startActivityForResult(pickIntent,REQUEST_SWIPE_DOWN_APP_CHOOSER);
			}
		}
		else if(preference.getKey().equals("homeBinding"))
		{
			// lets launch app picker if the user selected to launch an app on gesture
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER)))
			{
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	
	            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
	            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
	            startActivityForResult(pickIntent,REQUEST_HOME_BINDING_APP_CHOOSER);
			}
		}
		else if(preference.getKey().equals("swipeupActions"))
		{
			// lets launch app picker if the user selected to launch an app on gesture
			if (newValue.equals(String.valueOf(Launcher.BIND_APP_LAUNCHER)))
			{
				Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	
	            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
	            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
	            startActivityForResult(pickIntent,REQUEST_SWIPE_UP_APP_CHOOSER);
			}
		}
        return true;  
	}
	// wjax: Get the App chosen as to be launched upon gesture completion. And store it in SharedPreferences via AlmostNexusSettingsHelper!!!
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case REQUEST_SWIPE_DOWN_APP_CHOOSER:
					AlmostNexusSettingsHelper.setSwipeDownAppToLaunch(this, infoFromApplicationIntent(this, data));
				break;
				case REQUEST_HOME_BINDING_APP_CHOOSER:
					AlmostNexusSettingsHelper.setHomeBindingAppToLaunch(this, infoFromApplicationIntent(this, data));
				break;
				case REQUEST_SWIPE_UP_APP_CHOOSER:
					AlmostNexusSettingsHelper.setSwipeUpAppToLaunch(this, infoFromApplicationIntent(this, data));
				break;
			}
		}
		
	}
	
	// Extracts useful information from Intent containing app information
	private static ApplicationInfo infoFromApplicationIntent(Context context, Intent data) {
        ComponentName component = data.getComponent();
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = null;
        try {
            activityInfo = packageManager.getActivityInfo(component, 0 /* no flags */);
        } catch (NameNotFoundException e) {
        }

        if (activityInfo != null) {
            ApplicationInfo itemInfo = new ApplicationInfo();
            itemInfo.title = activityInfo.loadLabel(packageManager);
            if (itemInfo.title == null) {
                itemInfo.title = activityInfo.name;
            }

            itemInfo.setActivity(component, Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            itemInfo.icon = activityInfo.loadIcon(packageManager);
            itemInfo.container = ItemInfo.NO_ID;

            return itemInfo;
        }
        return null;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("highlights_color")) {
        	ColorPickerDialog cp = new ColorPickerDialog(this,mHighlightsColorListener,readHighlightsColor());
        	cp.show();
        }else if(preference.getKey().equals("highlights_color_focus")) {
        	ColorPickerDialog cp = new ColorPickerDialog(this,mHighlightsColorFocusListener,readHighlightsColorFocus());
        	cp.show();
        }else if(preference.getKey().equals("drawer_color")) {
        	ColorPickerDialog cp = new ColorPickerDialog(this,mDrawerColorListener,readDrawerColor());
        	cp.show();
        }
        return false;
	}
    private int readHighlightsColor() {
    	return AlmostNexusSettingsHelper.getHighlightsColor(this);
    }

    ColorPickerDialog.OnColorChangedListener mHighlightsColorListener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putInt("highlights_color", color).commit();
    	}
    };
    private int readHighlightsColorFocus() {
    	return AlmostNexusSettingsHelper.getHighlightsColorFocus(this);
    }

    ColorPickerDialog.OnColorChangedListener mHighlightsColorFocusListener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putInt("highlights_color_focus", color).commit();
    	}
    };
    private int readDrawerColor() {
    	return AlmostNexusSettingsHelper.getDrawerColor(this);
    }

    ColorPickerDialog.OnColorChangedListener mDrawerColorListener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putInt("drawer_color", color).commit();
    	}
    };
    
	// Wysie: Adapted from http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
    private class ExportPrefsTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        // can use UI thread here
        protected void onPreExecute() {
            this.dialog.setMessage(getResources().getString(R.string.xml_export_dialog));
            this.dialog.show();
        }

      // automatically done on worker thread (separate from UI thread)
        protected String doInBackground(final Void... args) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return getResources().getString(R.string.import_export_sdcard_unmounted);
            }

            File prefFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + 
                            "/shared_prefs/launcher.preferences.almostnexus.xml");            
            File file = new File(Environment.getExternalStorageDirectory(), PREF_BACKUP_FILENAME);

            try {
                file.createNewFile();
                copyFile(prefFile, file);
                return getResources().getString(R.string.xml_export_success);
            } catch (IOException e) {
                return getResources().getString(R.string.xml_export_error);
            }
        }

        // can use UI thread here
        protected void onPostExecute(final String msg) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

	// Wysie: Adapted from http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
    private class ImportPrefsTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        protected void onPreExecute() {
            this.dialog.setMessage(getResources().getString(R.string.xml_import_dialog));
            this.dialog.show();
        }

        // could pass the params used here in AsyncTask<String, Void, String> - but not being re-used
        protected String doInBackground(final Void... args) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return getResources().getString(R.string.import_export_sdcard_unmounted);
            }
            
            File prefBackupFile = new File(Environment.getExternalStorageDirectory(), PREF_BACKUP_FILENAME);
            
            if (!prefBackupFile.exists()) {
                return getResources().getString(R.string.xml_file_not_found);
            } else if (!prefBackupFile.canRead()) {
                return getResources().getString(R.string.xml_not_readable);
            }
            
            File prefFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + 
                            "/shared_prefs/launcher.preferences.almostnexus.xml");
            
            if (prefFile.exists()) {
                prefFile.delete();
            }

            try {
                prefFile.createNewFile();
                copyFile(prefBackupFile, prefFile);
                shouldRestart = true;
                return getResources().getString(R.string.xml_import_success);
            } catch (IOException e) {
                return getResources().getString(R.string.xml_import_error);
            }
        }

        protected void onPostExecute(final String msg) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }
	
	// Wysie: Adapted from http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
    private class ExportDatabaseTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        // can use UI thread here
        protected void onPreExecute() {
            this.dialog.setMessage(getResources().getString(R.string.dbfile_export_dialog));
            this.dialog.show();
        }

      // automatically done on worker thread (separate from UI thread)
        protected String doInBackground(final Void... args) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return getResources().getString(R.string.import_export_sdcard_unmounted);
            }

            File dbFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + "/databases/launcher.db");            
            File file = new File(Environment.getExternalStorageDirectory(), CONFIG_BACKUP_FILENAME);

            try {
                file.createNewFile();
                copyFile(dbFile, file);
                return getResources().getString(R.string.dbfile_export_success);
            } catch (IOException e) {
                return getResources().getString(R.string.dbfile_export_error);
            }
        }

        // can use UI thread here
        protected void onPostExecute(final String msg) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

	// Wysie: Adapted from http://code.google.com/p/and-examples/source/browse/#svn/trunk/database/src/com/totsp/database
    private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {
        private final ProgressDialog dialog = new ProgressDialog(mContext);

        protected void onPreExecute() {
            this.dialog.setMessage(getResources().getString(R.string.dbfile_import_dialog));
            this.dialog.show();
        }

        // could pass the params used here in AsyncTask<String, Void, String> - but not being re-used
        protected String doInBackground(final Void... args) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return getResources().getString(R.string.import_export_sdcard_unmounted);
            }
            
            File dbBackupFile = new File(Environment.getExternalStorageDirectory(), CONFIG_BACKUP_FILENAME);
            
            if (!dbBackupFile.exists()) {
                return getResources().getString(R.string.dbfile_not_found);
            } else if (!dbBackupFile.canRead()) {
                return getResources().getString(R.string.dbfile_not_readable);
            }
            
            File dbFile = new File(Environment.getDataDirectory() + "/data/" + NAMESPACE + "/databases/launcher.db");            
            
            if (dbFile.exists()) {
                dbFile.delete();
            }

            try {
                dbFile.createNewFile();
                copyFile(dbBackupFile, dbFile);
                shouldRestart = true;
                return getResources().getString(R.string.dbfile_import_success);
            } catch (IOException e) {
                return getResources().getString(R.string.dbfile_import_error);
            }
        }

        protected void onPostExecute(final String msg) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
        
        if (inChannel != null)
            inChannel.close();
        if (outChannel != null)
            outChannel.close();
        }
    }

}

