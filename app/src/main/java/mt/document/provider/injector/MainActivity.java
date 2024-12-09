
/*
 * MT-Document-Provider-Injector 
 * Copyright 2024, developer-krushna
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of developer-krushna nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


 *     Please contact Krushna by email mt.modder.hub@gmail.com if you need
 *     additional information or have any questions
 */
 
package mt.document.provider.injector;


import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import mt.document.provider.injector.util.*;
import mt.document.provider.injector.view.*;
import android.annotation.*;
import android.content.res.*;
import android.graphics.drawable.*;

/* 
 Author @developer-krushna
 */

public class MainActivity extends Activity {
	private EditText edit_path; // Input field for the file path
	private ImageView paste; // Button to paste content from the clipboard
	private Button Process; // Button to start processing the APK
	private LinearLayout main_linear; // Main layout for UI components

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main); // Set the layout file for the activity
		initialize(_savedInstanceState); // Initialize the UI components

		// Check and request storage permissions
		if (Build.VERSION.SDK_INT >= 23) { 
			if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[] {
									   Manifest.permission.READ_EXTERNAL_STORAGE, 
									   Manifest.permission.WRITE_EXTERNAL_STORAGE
								   }, 1000);
			} else {
				initializeLogic(); // Initialize additional logic if permissions are already granted
			}
		} else {
			initializeLogic(); // Initialize additional logic for older API versions
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) { 
			initializeLogic(); // Proceed with initialization after permissions are granted
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		// Find and link UI components by their IDs
		edit_path = findViewById(R.id.edit_path); 
		paste = findViewById(R.id.paste);
		Process = findViewById(R.id.Process);
		main_linear = findViewById(R.id.main_linear);

		// Set the click listener for the paste button
		paste.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					edit_path.setText(getClipboard(MainActivity.this)); // Get text from clipboard and set it in edit_path
				}
			});

		// Set the click listener for the Process button
		Process.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					start(MainActivity.this); // Start processing the APK
				}
			});
	}

	private void initializeLogic() {
		// Set ripple effect on the paste button
		ripple(paste, "#b2dfdb");
		// Apply rounded corners and a border to the main layout
		_RoundAndBorder(main_linear, "#FFFFFF", 3, "#F4386D", 8);
	}

	public void _RoundAndBorder(final View _view, final String _color1, final double _border, final String _color2, final double _round) {
		// Create a GradientDrawable to set background with rounded corners and a border
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
		gd.setColor(Color.parseColor(_color1));
		gd.setCornerRadius((int) _round);
		gd.setStroke((int) _border, Color.parseColor(_color2));
		_view.setBackground(gd);
	}

	public void ripple(View _view, String _c) {
		// Apply ripple effect on a view
		ColorStateList clr = new ColorStateList(
			new int[][]{new int[]{}},
			new int[]{Color.parseColor(_c)}
		); 
		RippleDrawable ripdr = new RippleDrawable(clr, null, null); 
		_view.setBackground(ripdr);
	}

    public static String getClipboard(Context context) {
        try {
            // Retrieve text from the clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData primaryClip = clipboard.getPrimaryClip();
            if (primaryClip != null && primaryClip.getItemCount() > 0) {
                return primaryClip.getItemAt(0).coerceToText(context).toString();
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle clipboard exceptions
        }
        return "";
    }

	public void start(final Activity activity) {
		// Create and show a progress dialog
		final AlertProgress progressDialog = new AlertProgress(activity);
		progressDialog.setTitle("Processing...");
		progressDialog.setMessage("AndroidManifest.xml");
		progressDialog.setIndeterminate(false);
		progressDialog.show();

		// Handler to update UI after background task completes
		final Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Reset screen-on flag
							progressDialog.dismiss(); // Dismiss the progress dialog
						}
					});
			}
		};

		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Prevent screen from sleeping
		new Thread() {
			public void run() {
				Looper.prepare();
				String srcApk = edit_path.getText().toString(); // Get the APK path from the input field
				try {
					// Process the APK file using DocumentInjector
					DocumentInjector injector = new DocumentInjector(new DocumentInjector.DocumentInjectorCallBack() {
							@Override
							public void onMessage(final String msg) {
								activity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											progressDialog.setMessage(msg); // Update progress message
										}
									});
							}

							@Override
							public void onProgress(final int progress, final int total) {
								activity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											progressDialog.setProgress(progress, total); // Update progress percentage
										}
									});
							}
						});
					injector.setPath(srcApk); // Set the APK file path
					injector.ProcessApk(); // Start APK processing
					Toast.makeText(activity, "Success, Sign it yourself", Toast.LENGTH_SHORT).show(); // Show success message
				} catch (Exception e) {
					showError(activity, e); // Handle errors
				}
				mHandler.sendEmptyMessage(0); // Notify the handler to dismiss the progress dialog
				Looper.loop();
			} 
		}.start();
	}
	
	
	public void showError(Context context, Exception e){
		try {
			final AlertDialog.Builder dlg = new AlertDialog.Builder(context);
			dlg.setTitle("Error");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String exceptionDetails = sw.toString();
			dlg.setMessage(exceptionDetails);
			dlg.setPositiveButton("Cancel", null);

			// Setting custom background
			int cornerRadius = 20;
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.parseColor("#FFFFFF"));
			gd.setCornerRadius(cornerRadius);

			final AlertDialog alert = dlg.create();

			// Set dialog width based on screen width percentage
			WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
			layoutParams.copyFrom(alert.getWindow().getAttributes());
			layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8); // 80% of screen width

			alert.getWindow().setBackgroundDrawable(gd);

			alert.show();

			final TextView message = alert.findViewById(android.R.id.message);
			message.setTextIsSelectable(true);
		} catch (WindowManager.BadTokenException e2) {
			e2.printStackTrace();
		}
	}
}

