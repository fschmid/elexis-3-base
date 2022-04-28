package ch.elexis.pdfBills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.mail.MailAccount;
import ch.elexis.core.mail.MailAccount.TYPE;
import ch.elexis.core.services.holder.ConfigServiceHolder;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.utils.CoreUtil;
import ch.elexis.data.Mandant;
import ch.elexis.data.Query;
import ch.elexis.pdfBills.print.PrintProcess;
import ch.elexis.pdfBills.print.PrinterSettingsDialog;
import ch.elexis.pdfBills.print.ScriptInitializer;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private TabFolder tabFolder;

	private TabItem t40Settings;
	private TabItem t44Settings;
	private TabItem mailConfig;
	private TabItem headerConfig;

	private Text headerLine1Text;
	private Text headerLine2Text;

	private Text reminderDays2Text;
	private Text reminderDays3Text;

	private Text mandantHeaderLine1Text;
	private Text mandantHeaderLine2Text;
	private Text mandantReminderDays2Text;
	private Text mandantReminderDays3Text;

	private Text printCommandText;

	private Label printerConfigLabel;
	private Label esrPrinterConfigLabel;

	private Button openDialogBtn;

	private Button openEsrDialogBtn;

	@Override
	public void init(IWorkbench workbench) {
		setTitle("PDF Rechnungsdruck");
		setDescription("Hier können die Einstellungen für den PDF Rechnungsdruck vorgenommen werden.");
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout());

		tabFolder = new TabFolder(ret, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		t44Settings = new TabItem(tabFolder, SWT.NONE);
		t44Settings.setText("Tarmed 4.4 / 4.5");
		createSettings(t44Settings, "4.4");
		getSettings(t44Settings);

		t40Settings = new TabItem(tabFolder, SWT.NONE);
		t40Settings.setText("Tarmed 4.0");
		createSettings(t40Settings, "4.0");
		getSettings(t40Settings);

		mailConfig = new TabItem(tabFolder, SWT.NONE);
		mailConfig.setText("Kopie per Mail");
		createMailConfig(mailConfig);

		headerConfig = new TabItem(tabFolder, SWT.NONE);
		headerConfig.setText("Rechnungskopf");
		createHeaderConfig(headerConfig);

		final Composite printerConfigComposite = new Composite(ret, SWT.NONE);
		printerConfigComposite.setLayout(new GridLayout(2, false));
		printerConfigComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Button doPrint = new Button(printerConfigComposite, SWT.CHECK);
		doPrint.setText("Direkter Druck");
		doPrint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		doPrint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CoreHub.localCfg.set(RnOutputter.CFG_PRINT_DIRECT, doPrint.getSelection());
				updatePrintDirect();
			}
		});
		doPrint.setSelection(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_DIRECT, false));

		Label label = new Label(printerConfigComposite, SWT.NONE);
		label.setText("Drucker Konfiguration");
		openDialogBtn = new Button(printerConfigComposite, SWT.PUSH);
		openDialogBtn.setText("konfigurieren");
		openDialogBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		openDialogBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PrinterSettingsDialog dialog = new PrinterSettingsDialog(Display.getDefault().getActiveShell(),
						CoreHub.localCfg.get(RnOutputter.CFG_PRINT_PRINTER, ""),
						CoreHub.localCfg.get(RnOutputter.CFG_PRINT_TRAY, ""));
				if (dialog.open() == Dialog.OK) {
					CoreHub.localCfg.set(RnOutputter.CFG_PRINT_PRINTER, dialog.getPrinter());
					CoreHub.localCfg.set(RnOutputter.CFG_PRINT_TRAY, dialog.getMediaTray());
					printerConfigLabel.setText(getPrinterConfigText());
					printerConfigComposite.layout();
				}
			}
		});

		printerConfigLabel = new Label(printerConfigComposite, SWT.NONE);
		printerConfigLabel.setText(getPrinterConfigText());
		printerConfigLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		label = new Label(printerConfigComposite, SWT.NONE);
		label.setText("ESR Drucker Konfiguration");
		openEsrDialogBtn = new Button(printerConfigComposite, SWT.PUSH);
		openEsrDialogBtn.setText("konfigurieren");
		openEsrDialogBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		openEsrDialogBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PrinterSettingsDialog dialog = new PrinterSettingsDialog(Display.getDefault().getActiveShell(),
						CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_PRINTER, ""),
						CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_TRAY, ""));
				if (dialog.open() == Dialog.OK) {
					CoreHub.localCfg.set(RnOutputter.CFG_ESR_PRINT_PRINTER, dialog.getPrinter());
					CoreHub.localCfg.set(RnOutputter.CFG_ESR_PRINT_TRAY, dialog.getMediaTray());
					esrPrinterConfigLabel.setText(getEsrPrinterConfigText());
					printerConfigComposite.layout();
				}
			}
		});

		esrPrinterConfigLabel = new Label(printerConfigComposite, SWT.NONE);
		esrPrinterConfigLabel.setText(getEsrPrinterConfigText());
		esrPrinterConfigLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		label = new Label(printerConfigComposite, SWT.NONE);
		label.setText("\nFolgende Variablen können in den Befehlen verwendet werden.\nVariablen: "
				+ PrintProcess.getVariablesAsString() + "\nZ.B.: befehl.exe -p [filename]\n");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		label = new Label(printerConfigComposite, SWT.NONE);
		label.setText("Befehl");
		printCommandText = new Text(printerConfigComposite, SWT.BORDER);
		printCommandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		printCommandText.setText(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_COMMAND, ""));

		if (CoreUtil.isWindows()) {
			final Button useScript = new Button(printerConfigComposite, SWT.CHECK);
			useScript.setText("Vordefinierte Scripts und Befehle verwenden.");
			if (CoreHub.localCfg.get(RnOutputter.CFG_PRINT_USE_SCRIPT, false)) {
				useScript.setSelection(true);
				printCommandText.setEditable(false);
			}
			useScript.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					CoreHub.localCfg.set(RnOutputter.CFG_PRINT_USE_SCRIPT, useScript.getSelection());
					if (useScript.getSelection() && StringUtils.isBlank(printCommandText.getText())) {
						Properties commandsProperties = ScriptInitializer
								.getPrintCommands("/rsc/script/win/printcommands.properties");
						if (commandsProperties != null && commandsProperties.get("printer") != null) {
							printCommandText.setText((String) commandsProperties.get("printer"));
						}
						printCommandText.setEditable(false);
					} else {
						printCommandText.setEditable(true);
					}
				}
			});
		}
		updatePrintDirect();

		label = new Label(ret, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final Button useGuarantorPostalAddress = new Button(ret, SWT.CHECK);
		useGuarantorPostalAddress
				.setText("Postanschrift des Garanten verwenden (Kann zu Abweichungen zwischen XML und PDF führen)");
		useGuarantorPostalAddress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		useGuarantorPostalAddress.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CoreHub.localCfg.set(RnOutputter.CFG_PRINT_USEGUARANTORPOSTAL,
						useGuarantorPostalAddress.getSelection());
				updatePrintDirect();
			}
		});
		useGuarantorPostalAddress.setSelection(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_USEGUARANTORPOSTAL, false));

		return ret;
	}

	private void createHeaderConfig(TabItem item) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Standard-Briefkopf, falls kein Mandanten-spezifischer konfiguriert wurde");
		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		label = new Label(composite, SWT.NONE);
		label.setText("ESR 1ste Zeile");
		headerLine1Text = new Text(composite, SWT.BORDER);
		headerLine1Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		headerLine1Text.setText(CoreHub.localCfg.get(RnOutputter.CFG_ESR_HEADER_1, ""));
		label = new Label(composite, SWT.NONE);
		label.setText("ESR 2te Zeile");
		headerLine2Text = new Text(composite, SWT.BORDER);
		headerLine2Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		headerLine2Text.setText(CoreHub.localCfg.get(RnOutputter.CFG_ESR_HEADER_2, ""));

		label = new Label(composite, SWT.NONE);
		label.setText("Mahnung 2 Tage");
		reminderDays2Text = new Text(composite, SWT.BORDER);
		reminderDays2Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		reminderDays2Text.setText(CoreHub.localCfg.get(RnOutputter.CFG_ESR_REMINDERDAYS_M2, "14"));
		label = new Label(composite, SWT.NONE);
		label.setText("Mahnung 3 Tage");
		reminderDays3Text = new Text(composite, SWT.BORDER);
		reminderDays3Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		reminderDays3Text.setText(CoreHub.localCfg.get(RnOutputter.CFG_ESR_REMINDERDAYS_M3, "14"));

		label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		label.setText("Mandanten-spezifischer Briefkopf");
		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		label = new Label(composite, SWT.NONE);
		label.setText("Mandant");
		final ComboViewer mandantsCombo = new ComboViewer(composite, SWT.BORDER);
		mandantsCombo.setContentProvider(ArrayContentProvider.getInstance());
		mandantsCombo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Mandant) {
					return ((Mandant) element).getLabel();
				}
				return super.getText(element);
			}
		});
		Query<Mandant> query = new Query<Mandant>(Mandant.class);
		List<Mandant> input = query.execute();
		Collections.sort(input, new MandantComparator());
		mandantsCombo.setInput(input);
		mandantsCombo.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Mandant mandant = (Mandant) event.getStructuredSelection().getFirstElement();
				mandantHeaderLine1Text.setText(
						ConfigServiceHolder.getGlobal(RnOutputter.CFG_ESR_HEADER_1 + "/" + mandant.getId(), ""));
				mandantHeaderLine2Text.setText(
						ConfigServiceHolder.getGlobal(RnOutputter.CFG_ESR_HEADER_2 + "/" + mandant.getId(), ""));
				mandantReminderDays2Text.setText(ConfigServiceHolder
						.getGlobal(RnOutputter.CFG_ESR_REMINDERDAYS_M2 + "/" + mandant.getId(), "14"));
				mandantReminderDays3Text.setText(ConfigServiceHolder
						.getGlobal(RnOutputter.CFG_ESR_REMINDERDAYS_M3 + "/" + mandant.getId(), "14"));
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("ESR 1ste Zeile");
		mandantHeaderLine1Text = new Text(composite, SWT.BORDER);
		mandantHeaderLine1Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mandantHeaderLine1Text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mandant mandant = (Mandant) ((IStructuredSelection) mandantsCombo.getSelection()).getFirstElement();
				if (mandant != null) {
					ConfigServiceHolder.setGlobal(RnOutputter.CFG_ESR_HEADER_1 + "/" + mandant.getId(),
							mandantHeaderLine1Text.getText());
				}
			}
		});
		label = new Label(composite, SWT.NONE);
		label.setText("ESR 2te Zeile");
		mandantHeaderLine2Text = new Text(composite, SWT.BORDER);
		mandantHeaderLine2Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mandantHeaderLine2Text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mandant mandant = (Mandant) ((IStructuredSelection) mandantsCombo.getSelection()).getFirstElement();
				if (mandant != null) {
					ConfigServiceHolder.setGlobal(RnOutputter.CFG_ESR_HEADER_2 + "/" + mandant.getId(),
							mandantHeaderLine2Text.getText());
				}
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("Mahnung 2 Tage");
		mandantReminderDays2Text = new Text(composite, SWT.BORDER);
		mandantReminderDays2Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mandantReminderDays2Text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mandant mandant = (Mandant) ((IStructuredSelection) mandantsCombo.getSelection()).getFirstElement();
				if (mandant != null) {
					ConfigServiceHolder.setGlobal(RnOutputter.CFG_ESR_REMINDERDAYS_M2 + "/" + mandant.getId(),
							mandantReminderDays2Text.getText());
				}
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("Mahnung 3 Tage");
		mandantReminderDays3Text = new Text(composite, SWT.BORDER);
		mandantReminderDays3Text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mandantReminderDays3Text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Mandant mandant = (Mandant) ((IStructuredSelection) mandantsCombo.getSelection()).getFirstElement();
				if (mandant != null) {
					ConfigServiceHolder.setGlobal(RnOutputter.CFG_ESR_REMINDERDAYS_M3 + "/" + mandant.getId(),
							mandantReminderDays3Text.getText());
				}
			}
		});

		item.setControl(composite);
	}

	private void createMailConfig(TabItem item) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Konfiguration des mail accounts für die Übermittlung der Rechnungskopie");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Query<Mandant> query = new Query<Mandant>(Mandant.class);
		List<Mandant> mandants = query.execute();
		Collections.sort(mandants, new MandantComparator());
		for (final Mandant mandant : mandants) {
			if (mandant.isInactive()) {
				continue;
			}
			label = new Label(composite, SWT.NONE);
			label.setText("Mandant " + mandant.getLabel());

			ComboViewer accountsViewer = new ComboViewer(composite, SWT.BORDER);
			accountsViewer.setContentProvider(ArrayContentProvider.getInstance());
			accountsViewer.setLabelProvider(new LabelProvider());
			final List<String> manadantAccounts = getSendMailAccounts(mandant);
			accountsViewer.setInput(manadantAccounts);
			accountsViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			accountsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object selected = event.getStructuredSelection().getFirstElement();
					if (selected instanceof String && StringUtils.isNotBlank((String) selected)) {
						ConfigServiceHolder.setGlobal(
								RnOutputter.CFG_ROOT + RnOutputter.CFG_MAIL_MANDANT_ACCOUNT + "/" + mandant.getId(),
								(String) selected);
					}
				}
			});
			String selectedAccount = ConfigServiceHolder.getGlobal(
					RnOutputter.CFG_ROOT + RnOutputter.CFG_MAIL_MANDANT_ACCOUNT + "/" + mandant.getId(), null);
			if (StringUtils.isNotBlank(selectedAccount)) {
				accountsViewer.setSelection(new StructuredSelection(selectedAccount));
			}
		}

		item.setControl(composite);
	}

	private List<String> getSendMailAccounts(Mandant mandant) {
		List<String> ret = new ArrayList<String>();
		List<String> accounts = MailClientHolder.get().getAccounts();
		for (String accountId : accounts) {
			Optional<MailAccount> accountOptional = MailClientHolder.get().getAccount(accountId);
			if (accountOptional.isPresent()) {
				if (accountOptional.get().getType() == TYPE.SMTP) {
					ret.add(accountId);
				}
			}
		}
		return ret;
	}

	private void updatePrintDirect() {
		openDialogBtn.setEnabled(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_DIRECT, false));
		openEsrDialogBtn.setEnabled(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_DIRECT, false));
		printCommandText.setEnabled(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_DIRECT, false));
	}

	private String getEsrPrinterConfigText() {
		StringBuilder sb = new StringBuilder();
		if (!CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_PRINTER, "").isEmpty()) {
			sb.append(CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_PRINTER, ""));
		} else {
			sb.append("Kein Drucker");
		}
		if (!CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_TRAY, "").isEmpty()) {
			sb.append(", Fach ").append(CoreHub.localCfg.get(RnOutputter.CFG_ESR_PRINT_TRAY, ""));
		} else {
			sb.append(", Kein Fach");
		}
		return sb.toString();
	}

	private String getPrinterConfigText() {
		StringBuilder sb = new StringBuilder();
		if (!CoreHub.localCfg.get(RnOutputter.CFG_PRINT_PRINTER, "").isEmpty()) {
			sb.append(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_PRINTER, ""));
		} else {
			sb.append("Kein Drucker");
		}
		if (!CoreHub.localCfg.get(RnOutputter.CFG_PRINT_TRAY, "").isEmpty()) {
			sb.append(", Fach ").append(CoreHub.localCfg.get(RnOutputter.CFG_PRINT_TRAY, ""));
		} else {
			sb.append(", Kein Fach");
		}
		return sb.toString();
	}

	@Override
	public boolean performOk() {
		saveSettings(t40Settings);
		saveSettings(t44Settings);
		return super.performOk();
	}

	private void saveSettings(TabItem item) {
		Composite composite = (Composite) item.getControl();
		for (Control control : composite.getChildren()) {
			if (control instanceof Text && control.getData() instanceof String) {
				Text text = (Text) control;
				String cfgKey = (String) control.getData();
				String value = text.getText();
				if (value != null && !value.isEmpty() && checkValue(value)) {
					CoreHub.localCfg.set(cfgKey, value);
				}
			}
		}
		CoreHub.localCfg.set(RnOutputter.CFG_ESR_HEADER_1, headerLine1Text.getText());
		CoreHub.localCfg.set(RnOutputter.CFG_ESR_HEADER_2, headerLine2Text.getText());

		CoreHub.localCfg.set(RnOutputter.CFG_ESR_REMINDERDAYS_M2, reminderDays2Text.getText());
		CoreHub.localCfg.set(RnOutputter.CFG_ESR_REMINDERDAYS_M3, reminderDays3Text.getText());

		CoreHub.localCfg.set(RnOutputter.CFG_PRINT_COMMAND, printCommandText.getText());
		CoreHub.localCfg.flush();
	}

	public boolean checkValue(String value) {
		try {
			Float.parseFloat(value);
			return true;
		} catch (NumberFormatException e) {
			// s is not numeric
			return false;
		}
	}

	private void getSettings(TabItem item) {
		Composite composite = (Composite) item.getControl();
		for (Control control : composite.getChildren()) {
			if (control instanceof Text && control.getData() instanceof String) {
				Text text = (Text) control;
				String cfgKey = (String) control.getData();
				text.setText(getSetting(cfgKey));
			}
		}
	}

	public static String getSetting(String cfgKey) {
		return CoreHub.localCfg.get(cfgKey, getDefault(cfgKey));
	}

	private static String getDefault(String cfgKey) {
		// first try old settings ... then default
		String oldKey = cfgKey.replaceFirst("4.4/", "").replaceFirst("4.0/", "");
		String oldSetting = CoreHub.localCfg.get(oldKey, null);
		if (oldSetting != null) {
			return oldSetting;
		} else {
			if (cfgKey.endsWith(RnOutputter.CFG_MARGINLEFT)) {
				return "1.5";
			} else if (cfgKey.endsWith(RnOutputter.CFG_MARGINRIGHT)) {
				return "0.7";
			} else if (cfgKey.endsWith(RnOutputter.CFG_MARGINTOP)) {
				return "1";
			} else if (cfgKey.endsWith(RnOutputter.CFG_MARGINBOTTOM)) {
				return "1.5";
			} else if (cfgKey.endsWith(RnOutputter.CFG_BESR_MARGIN_VERTICAL)) {
				return "0.75";
			} else if (cfgKey.endsWith(RnOutputter.CFG_BESR_MARGIN_HORIZONTAL)) {
				return "0.0";
			}
		}
		return "";
	}

	private void createSettings(TabItem item, String version) {
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout());
		new Label(composite, 256).setText("Rand links [cm]");
		Text pdfLeftMargin = new Text(composite, 128);
		pdfLeftMargin.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfLeftMargin.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_MARGINLEFT);
		new Label(composite, 512).setText("Rand rechts [cm]");
		Text pdfRightMargin = new Text(composite, 128);
		pdfRightMargin.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfRightMargin.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_MARGINRIGHT);
		new Label(composite, 1024).setText("Rand oben [cm]");
		Text pdfTopMargin = new Text(composite, 128);
		pdfTopMargin.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfTopMargin.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_MARGINTOP);
		new Label(composite, 128).setText("Rand unten [cm]");
		Text pdfBottumMagin = new Text(composite, 128);
		pdfBottumMagin.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfBottumMagin.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_MARGINBOTTOM);
		new Label(composite, 128).setText("BESR Abstand zu Rand unten [cm]");
		Text pdfBesrMarginVertical = new Text(composite, 128);
		pdfBesrMarginVertical.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfBesrMarginVertical.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_BESR_MARGIN_VERTICAL);
		new Label(composite, 128).setText("BESR Abstand zu Rand rechts [cm]");
		Text pdfBesrMarginHorizontal = new Text(composite, 128);
		pdfBesrMarginHorizontal.setLayoutData(SWTHelper.getFillGridData(2, true, 2, false));
		pdfBesrMarginHorizontal.setData(RnOutputter.CFG_ROOT + version + "/" + RnOutputter.CFG_BESR_MARGIN_HORIZONTAL);
		item.setControl(composite);
	}

	private class MandantComparator implements Comparator<Mandant> {
		@Override
		public int compare(Mandant l, Mandant r) {
			return l.getLabel().compareTo(r.getLabel());
		}
	}
}
