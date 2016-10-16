package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class ScheduledRestartingOptionsDialog extends Dialog {
	
	protected Response		result			= Response.NO_RESPONSE;
	protected Shell			shell;
	protected DateTime		dateTime_1;
	protected DateTime		dateTime_2;
	protected DateTime		dateTime_3;
	protected DateTime		dateTime_4;
	protected DateTime		dateTime_5;
	protected DateTime		dateTime_6;
	protected Button		btnTime1;
	protected Button		btnTime2;
	protected Button		btnTime3;
	protected Button		btnTime4;
	protected Button		btnTime5;
	protected Button		btnTime6;
	private Button			btnMonday;
	private Button			btnTuesday;
	private Button			btnWednesday;
	private Button			btnThursday;
	private Button			btnFriday;
	private Button			btnSaturday;
	private Button			btnSunday;
	private Label			label_1;
	private Text			restartCommands;
	
	public final TimeResult	timeData;
	public volatile String	commandsToRun	= "";
	private Label			label_2;
	private Button			btnCancel;
	
	/** Create the dialog.
	 * 
	 * @param parent The parent Shell
	 * @param timeData The old TimeResult data
	 * @param commandsToRun The old commands to run */
	public ScheduledRestartingOptionsDialog(Shell parent, TimeResult timeData, String commandsToRun) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		this.timeData = timeData;
		this.commandsToRun = commandsToRun;
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		createContents();
		if(!Main.headless) {
			this.shell.open();
			this.shell.layout();
			while(!this.shell.isDisposed()) {
				this.updateUI();
				Main.mainLoop();
				if(this.result != Response.NO_RESPONSE) {
					break;
				}
			}
			if(!this.shell.isDisposed()) {
				this.shell.dispose();
			}
		}
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				ScheduledRestartingOptionsDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.keyCode == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setSize(360, 382);
		this.shell.setText("Scheduled Restarting Options");
		this.shell.setImages(Main.getDefaultImages());
		Functions.centerShell2OnShell1(getParent(), this.shell);
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ScheduledRestartingOptionsDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(10, 321, 164, 23);
		btnDone.setText("Done");
		
		this.btnCancel = new Button(this.shell, SWT.NONE);
		this.btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ScheduledRestartingOptionsDialog.this.result = Response.CANCEL;
			}
		});
		this.btnCancel.setBounds(180, 321, 163, 23);
		this.btnCancel.setText("Cancel");
		
		Label lblRestartTheServer = new Label(this.shell, SWT.NONE);
		lblRestartTheServer.setBounds(10, 10, 136, 13);
		lblRestartTheServer.setText("Restart the server every:");
		
		this.btnMonday = new Button(this.shell, SWT.CHECK);
		this.btnMonday.setBounds(10, 29, 68, 16);
		this.btnMonday.setText("Monday");
		
		this.btnTuesday = new Button(this.shell, SWT.CHECK);
		this.btnTuesday.setBounds(84, 29, 68, 16);
		this.btnTuesday.setText("Tuesday");
		
		this.btnWednesday = new Button(this.shell, SWT.CHECK);
		this.btnWednesday.setBounds(158, 29, 85, 16);
		this.btnWednesday.setText("Wednesday");
		
		this.btnThursday = new Button(this.shell, SWT.CHECK);
		this.btnThursday.setBounds(249, 29, 74, 16);
		this.btnThursday.setText("Thursday");
		
		this.btnFriday = new Button(this.shell, SWT.CHECK);
		this.btnFriday.setBounds(10, 51, 62, 16);
		this.btnFriday.setText("Friday");
		
		this.btnSaturday = new Button(this.shell, SWT.CHECK);
		this.btnSaturday.setBounds(84, 51, 74, 16);
		this.btnSaturday.setText("Saturday");
		
		this.btnSunday = new Button(this.shell, SWT.CHECK);
		this.btnSunday.setBounds(168, 51, 74, 16);
		this.btnSunday.setText("Sunday");
		
		Label lblAt = new Label(this.shell, SWT.NONE);
		lblAt.setBounds(10, 81, 26, 13);
		lblAt.setText("at:");
		
		this.btnTime1 = new Button(this.shell, SWT.CHECK);
		this.btnTime1.setBounds(10, 100, 16, 23);
		
		Label label = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBounds(10, 73, 333, 2);
		
		this.dateTime_1 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_1.setBounds(32, 100, 85, 23);
		this.dateTime_1.setTime(4, 30, 00);
		
		this.btnTime2 = new Button(this.shell, SWT.CHECK);
		this.btnTime2.setBounds(123, 100, 16, 23);
		
		this.dateTime_2 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_2.setBounds(145, 100, 85, 23);
		this.dateTime_2.setTime(8, 30, 00);
		
		this.btnTime3 = new Button(this.shell, SWT.CHECK);
		this.btnTime3.setBounds(236, 100, 16, 23);
		
		this.dateTime_3 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_3.setBounds(258, 100, 85, 23);
		this.dateTime_3.setTime(12, 30, 00);
		
		this.btnTime4 = new Button(this.shell, SWT.CHECK);
		this.btnTime4.setBounds(10, 129, 16, 23);
		
		this.dateTime_4 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_4.setBounds(32, 129, 85, 23);
		this.dateTime_4.setTime(16, 30, 00);
		
		this.btnTime5 = new Button(this.shell, SWT.CHECK);
		this.btnTime5.setBounds(123, 129, 16, 23);
		
		this.dateTime_5 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_5.setBounds(145, 129, 85, 23);
		this.dateTime_5.setTime(20, 30, 00);
		
		this.btnTime6 = new Button(this.shell, SWT.CHECK);
		this.btnTime6.setBounds(236, 129, 16, 23);
		
		this.dateTime_6 = new DateTime(this.shell, SWT.BORDER | SWT.TIME);
		this.dateTime_6.setBounds(258, 129, 85, 23);
		
		this.label_1 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.label_1.setBounds(10, 158, 333, 2);
		
		Label lblandRunThe = new Label(this.shell, SWT.WRAP);
		lblandRunThe.setBounds(10, 166, 333, 33);
		lblandRunThe.setText("...and run the following commands before restarting(separate each command with a new line):");
		
		this.restartCommands = new Text(this.shell, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		this.restartCommands.setBounds(10, 205, 334, 102);
		
		this.label_2 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.label_2.setBounds(10, 313, 333, 2);
		this.dateTime_6.setTime(00, 30, 00);
		
		//==========
		
		this.btnMonday.setSelection(this.timeData.monday);
		this.btnTuesday.setSelection(this.timeData.tuesday);
		this.btnWednesday.setSelection(this.timeData.wednesday);
		this.btnThursday.setSelection(this.timeData.thursday);
		this.btnFriday.setSelection(this.timeData.friday);
		this.btnSaturday.setSelection(this.timeData.saturday);
		this.btnSunday.setSelection(this.timeData.sunday);
		
		this.btnTime1.setSelection(this.timeData.enable1);
		this.btnTime2.setSelection(this.timeData.enable2);
		this.btnTime3.setSelection(this.timeData.enable3);
		this.btnTime4.setSelection(this.timeData.enable4);
		this.btnTime5.setSelection(this.timeData.enable5);
		this.btnTime6.setSelection(this.timeData.enable6);
		
		this.dateTime_1.setHours(this.timeData.hour1);
		this.dateTime_2.setHours(this.timeData.hour2);
		this.dateTime_3.setHours(this.timeData.hour3);
		this.dateTime_4.setHours(this.timeData.hour4);
		this.dateTime_5.setHours(this.timeData.hour5);
		this.dateTime_6.setHours(this.timeData.hour6);
		
		this.dateTime_1.setMinutes(this.timeData.minute1);
		this.dateTime_2.setMinutes(this.timeData.minute2);
		this.dateTime_3.setMinutes(this.timeData.minute3);
		this.dateTime_4.setMinutes(this.timeData.minute4);
		this.dateTime_5.setMinutes(this.timeData.minute5);
		this.dateTime_6.setMinutes(this.timeData.minute6);
		
		this.dateTime_1.setSeconds(this.timeData.second1);
		this.dateTime_2.setSeconds(this.timeData.second2);
		this.dateTime_3.setSeconds(this.timeData.second3);
		this.dateTime_4.setSeconds(this.timeData.second4);
		this.dateTime_5.setSeconds(this.timeData.second5);
		this.dateTime_6.setSeconds(this.timeData.second6);
		
		this.restartCommands.setText(this.commandsToRun);
	}
	
	private final void updateUI() {
		this.timeData.enable1 = this.btnTime1.getSelection();
		this.timeData.enable2 = this.btnTime2.getSelection();
		this.timeData.enable3 = this.btnTime3.getSelection();
		this.timeData.enable4 = this.btnTime4.getSelection();
		this.timeData.enable5 = this.btnTime5.getSelection();
		this.timeData.enable6 = this.btnTime6.getSelection();
		
		this.timeData.monday = this.btnMonday.getSelection();
		this.timeData.tuesday = this.btnTuesday.getSelection();
		this.timeData.wednesday = this.btnWednesday.getSelection();
		this.timeData.thursday = this.btnThursday.getSelection();
		this.timeData.friday = this.btnFriday.getSelection();
		this.timeData.saturday = this.btnSaturday.getSelection();
		this.timeData.sunday = this.btnSunday.getSelection();
		
		this.dateTime_1.setEnabled(this.timeData.enable1);
		this.dateTime_2.setEnabled(this.timeData.enable2);
		this.dateTime_3.setEnabled(this.timeData.enable3);
		this.dateTime_4.setEnabled(this.timeData.enable4);
		this.dateTime_5.setEnabled(this.timeData.enable5);
		this.dateTime_6.setEnabled(this.timeData.enable6);
		
		this.timeData.hour1 = this.dateTime_1.getHours();
		this.timeData.hour2 = this.dateTime_2.getHours();
		this.timeData.hour3 = this.dateTime_3.getHours();
		this.timeData.hour4 = this.dateTime_4.getHours();
		this.timeData.hour5 = this.dateTime_5.getHours();
		this.timeData.hour6 = this.dateTime_6.getHours();
		
		this.timeData.minute1 = this.dateTime_1.getMinutes();
		this.timeData.minute2 = this.dateTime_2.getMinutes();
		this.timeData.minute3 = this.dateTime_3.getMinutes();
		this.timeData.minute4 = this.dateTime_4.getMinutes();
		this.timeData.minute5 = this.dateTime_5.getMinutes();
		this.timeData.minute6 = this.dateTime_6.getMinutes();
		
		this.timeData.second1 = this.dateTime_1.getSeconds();
		this.timeData.second2 = this.dateTime_2.getSeconds();
		this.timeData.second3 = this.dateTime_3.getSeconds();
		this.timeData.second4 = this.dateTime_4.getSeconds();
		this.timeData.second5 = this.dateTime_5.getSeconds();
		this.timeData.second6 = this.dateTime_6.getSeconds();
		
		this.commandsToRun = this.restartCommands.getText();
	}
	
	public static final class TimeResult {
		public volatile boolean	monday		= false;
		public volatile boolean	tuesday		= false;
		public volatile boolean	wednesday	= false;
		public volatile boolean	thursday	= false;
		public volatile boolean	friday		= false;
		public volatile boolean	saturday	= false;
		public volatile boolean	sunday		= false;
		
		public volatile boolean	enable1		= false;
		public volatile boolean	enable2		= false;
		public volatile boolean	enable3		= false;
		public volatile boolean	enable4		= false;
		public volatile boolean	enable5		= false;
		public volatile boolean	enable6		= false;
		
		public volatile int		hour1		= 0;
		public volatile int		hour2		= 4;
		public volatile int		hour3		= 8;
		public volatile int		hour4		= 12;
		public volatile int		hour5		= 16;
		public volatile int		hour6		= 20;
		
		public volatile int		minute1		= 30;
		public volatile int		minute2		= 30;
		public volatile int		minute3		= 30;
		public volatile int		minute4		= 30;
		public volatile int		minute5		= 30;
		public volatile int		minute6		= 30;
		
		public volatile int		second1		= 0;
		public volatile int		second2		= 0;
		public volatile int		second3		= 0;
		public volatile int		second4		= 0;
		public volatile int		second5		= 0;
		public volatile int		second6		= 0;
		
		public TimeResult() {
		}
		
		public TimeResult(TimeResult copy) {
			this.monday = copy.monday;
			this.tuesday = copy.tuesday;
			this.wednesday = copy.wednesday;
			this.thursday = copy.thursday;
			this.friday = copy.friday;
			this.saturday = copy.saturday;
			this.sunday = copy.sunday;
			
			this.enable1 = copy.enable1;
			this.enable2 = copy.enable2;
			this.enable3 = copy.enable3;
			this.enable4 = copy.enable4;
			this.enable5 = copy.enable5;
			this.enable6 = copy.enable6;
			
			this.hour1 = copy.hour1;
			this.hour2 = copy.hour2;
			this.hour3 = copy.hour3;
			this.hour4 = copy.hour4;
			this.hour5 = copy.hour5;
			this.hour6 = copy.hour6;
			
			this.minute1 = copy.minute1;
			this.minute2 = copy.minute2;
			this.minute3 = copy.minute3;
			this.minute4 = copy.minute4;
			this.minute5 = copy.minute5;
			this.minute6 = copy.minute6;
			
			this.second1 = copy.second1;
			this.second2 = copy.second2;
			this.second3 = copy.second3;
			this.second4 = copy.second4;
			this.second5 = copy.second5;
			this.second6 = copy.second6;
		}
		
		@Override
		public final String toString() {
			String rtrn = "monday=" + this.monday + "\r\n";
			rtrn += "tuesday=" + this.tuesday + "\r\n";
			rtrn += "wednesday=" + this.wednesday + "\r\n";
			rtrn += "thursday=" + this.thursday + "\r\n";
			rtrn += "friday=" + this.friday + "\r\n";
			rtrn += "saturday=" + this.saturday + "\r\n";
			rtrn += "sunday=" + this.sunday + "\r\n";
			rtrn += "enable1=" + this.enable1 + "\r\n";
			rtrn += "enable2=" + this.enable2 + "\r\n";
			rtrn += "enable3=" + this.enable3 + "\r\n";
			rtrn += "enable4=" + this.enable4 + "\r\n";
			rtrn += "enable5=" + this.enable5 + "\r\n";
			rtrn += "enable6=" + this.enable6 + "\r\n";
			rtrn += "hour1=" + this.hour1 + "\r\n";
			rtrn += "hour2=" + this.hour2 + "\r\n";
			rtrn += "hour3=" + this.hour3 + "\r\n";
			rtrn += "hour4=" + this.hour4 + "\r\n";
			rtrn += "hour5=" + this.hour5 + "\r\n";
			rtrn += "hour6=" + this.hour6 + "\r\n";
			rtrn += "minute1=" + this.minute1 + "\r\n";
			rtrn += "minute2=" + this.minute2 + "\r\n";
			rtrn += "minute3=" + this.minute3 + "\r\n";
			rtrn += "minute4=" + this.minute4 + "\r\n";
			rtrn += "minute5=" + this.minute5 + "\r\n";
			rtrn += "minute6=" + this.minute6 + "\r\n";
			rtrn += "second1=" + this.second1 + "\r\n";
			rtrn += "second2=" + this.second2 + "\r\n";
			rtrn += "second3=" + this.second3 + "\r\n";
			rtrn += "second4=" + this.second4 + "\r\n";
			rtrn += "second5=" + this.second5 + "\r\n";
			rtrn += "second6=" + this.second6;
			return rtrn;
		}
		
	}
	
}
