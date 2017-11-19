package ch.elexis.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;
import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.elexis.core.data.interfaces.IVerrechenbar;
import ch.elexis.core.data.util.MultiplikatorList;
import ch.elexis.data.TarmedLeistung.MandantType;
import ch.elexis.data.importer.TarmedReferenceDataImporter;
import ch.rgw.tools.Money;
import ch.rgw.tools.Result;
import ch.rgw.tools.TimeTool;

public class TarmedOptifierTest {
	private static TarmedOptifier optifier;
	private static Patient patGrissemann, patStermann, patOneYear;
	private static Konsultation konsGriss, konsSter, konsOneYear;
	private static TarmedLeistung tlBaseFirst5Min, tlBaseXRay, tlBaseRadiologyHospital,
			tlUltrasound, tlTapingCat1, tlAgeTo1Month, tlAgeTo7Years, tlAgeFrom7Years;
			
	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		optifier = new TarmedOptifier();
		
		importTarmedReferenceData();
		
		// init some basic services
		tlBaseFirst5Min = (TarmedLeistung) TarmedLeistung.getFromCode("00.0010");
		tlBaseXRay = (TarmedLeistung) TarmedLeistung.getFromCode("39.0020");
		tlBaseRadiologyHospital = (TarmedLeistung) TarmedLeistung.getFromCode("39.0015");
		tlUltrasound = (TarmedLeistung) TarmedLeistung.getFromCode("39.3005");
		tlTapingCat1 = (TarmedLeistung) TarmedLeistung.getFromCode("01.0110");
		
		tlAgeTo1Month = (TarmedLeistung) TarmedLeistung.getFromCode("00.0870");
		tlAgeTo7Years = (TarmedLeistung) TarmedLeistung.getFromCode("00.0900");
		tlAgeFrom7Years = (TarmedLeistung) TarmedLeistung.getFromCode("00.0890");
		
		//Patient Grissemann with case and consultation
		patGrissemann = new Patient("Grissemann", "Christoph", "17.05.1966", Patient.MALE);
		Fall fallGriss = patGrissemann.neuerFall("Testfall Grissemann", Fall.getDefaultCaseReason(),
			Fall.getDefaultCaseLaw());
		fallGriss.setInfoElement("Kostenträger", patGrissemann.getId());
		konsGriss = new Konsultation(fallGriss);
		konsGriss.addDiagnose(TICode.getFromCode("T1"));
		konsGriss.addLeistung(tlBaseFirst5Min);
		
		//Patient Stermann with case and consultation
		patStermann = new Patient("Stermann", "Dirk", "07.12.1965", Patient.MALE);
		Fall fallSter = patStermann.neuerFall("Testfall Stermann", Fall.getDefaultCaseReason(),
			Fall.getDefaultCaseLaw());
		fallSter.setInfoElement("Kostenträger", patStermann.getId());
		konsSter = new Konsultation(fallSter);
		konsSter.addDiagnose(TICode.getFromCode("T1"));
		konsSter.addLeistung(tlBaseFirst5Min);
		
		//Patient OneYear with case and consultation
		String dob = LocalDate.now().minusYears(1).minusDays(1)
			.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		patOneYear = new Patient("One", "Year", dob, Patient.MALE);
		Fall fallOneYear = patOneYear.neuerFall("Testfall One", Fall.getDefaultCaseReason(),
			Fall.getDefaultCaseLaw());
		fallSter.setInfoElement("Kostenträger", patOneYear.getId());
		konsOneYear = new Konsultation(fallOneYear);
		konsOneYear.addDiagnose(TICode.getFromCode("T1"));
		konsOneYear.addLeistung(tlBaseFirst5Min);
		
	}
	
	private static void importTarmedReferenceData() throws FileNotFoundException{
		File tarmedFile = new File(System.getProperty("user.dir") + File.separator + "rsc"
			+ File.separator + "tarmed.mdb");
		InputStream tarmedInStream = new FileInputStream(tarmedFile);
		
		TarmedReferenceDataImporter importer = new TarmedReferenceDataImporter();
		importer.suppressRestartDialog();
		Status retStatus =
			(Status) importer.performImport(new NullProgressMonitor(), tarmedInStream, null);
		assertEquals(IStatus.OK, retStatus.getCode());
	}
	
	private TarmedLeistung additionalService;
	private TarmedLeistung mainService;
	
	@Test
	public void testAddCompatibleAndIncompatible(){
		Result<IVerrechenbar> resultGriss = optifier.add(tlUltrasound, konsGriss);
		assertTrue(resultGriss.isOK());
		resultGriss = optifier.add(tlBaseXRay, konsGriss);
		assertFalse(resultGriss.isOK());
		resultGriss = optifier.add(tlTapingCat1, konsGriss);
		assertTrue(resultGriss.isOK());
	}
	
	@Test
	public void testAddMultipleIncompatible(){
		Result<IVerrechenbar> resultSter = optifier.add(tlBaseXRay, konsSter);
		assertTrue(resultSter.isOK());
		resultSter = optifier.add(tlUltrasound, konsSter);
		assertFalse(resultSter.isOK());
		resultSter = optifier.add(tlBaseRadiologyHospital, konsSter);
		assertFalse(resultSter.isOK());
	}
	
	@Test
	public void testIsCompatible(){
		Result<IVerrechenbar> resCompatible = optifier.isCompatible(tlBaseXRay, tlUltrasound);
		assertFalse(resCompatible.isOK());
		String resText = "";
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("39.3005 nicht kombinierbar mit 39.0020", resText);
		resCompatible = optifier.isCompatible(tlUltrasound, tlBaseXRay);
		assertTrue(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseXRay, tlBaseRadiologyHospital);
		assertFalse(resCompatible.isOK());
		if (!resCompatible.getMessages().isEmpty()) {
			resText = resCompatible.getMessages().get(0).getText();
		}
		assertEquals("39.0015 nicht kombinierbar mit 39.0020", resText);
		
		resCompatible = optifier.isCompatible(tlBaseRadiologyHospital, tlUltrasound);
		assertFalse(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseXRay, tlBaseFirst5Min);
		assertTrue(resCompatible.isOK());
		
		resCompatible = optifier.isCompatible(tlBaseFirst5Min, tlBaseRadiologyHospital);
		assertTrue(resCompatible.isOK());
	}
	
	@Test
	public void testSetBezug(){
		additionalService = (TarmedLeistung) TarmedLeistung.getFromCode("39.5010");
		mainService = (TarmedLeistung) TarmedLeistung.getFromCode("39.5060");
		// additional without main, not allowed
		Result<IVerrechenbar> resultSter = optifier.add(additionalService, konsSter);
		assertFalse(resultSter.isOK());
		// additional after main, allowed
		resultSter = optifier.add(mainService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, mainService).isPresent());
		
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// another additional, not allowed
		resultSter = optifier.add(additionalService, konsSter);
		assertFalse(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// remove, and add again
		Optional<Verrechnet> verrechnet = getVerrechent(konsSter, additionalService);
		assertTrue(verrechnet.isPresent());
		Result<Verrechnet> result = optifier.remove(verrechnet.get(), konsSter);
		assertTrue(result.isOK());
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		// add another main and additional
		resultSter = optifier.add(mainService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, mainService).isPresent());
		
		resultSter = optifier.add(additionalService, konsSter);
		assertTrue(resultSter.isOK());
		assertTrue(getVerrechent(konsSter, additionalService).isPresent());
		
		// remove main service, should also remove additional service
		verrechnet = getVerrechent(konsSter, mainService);
		result = optifier.remove(verrechnet.get(), konsSter);
		assertTrue(result.isOK());
		assertFalse(getVerrechent(konsSter, mainService).isPresent());
		assertFalse(getVerrechent(konsSter, additionalService).isPresent());
	}
	
	@Test
	public void testOneYear(){
		// additional without main, not allowed
		Result<IVerrechenbar> result = optifier.add(tlAgeTo1Month, konsOneYear);
		assertFalse(result.isOK());
		
		result = optifier.add(tlAgeTo7Years, konsOneYear);
		assertTrue(result.isOK());
		
		result = optifier.add(tlAgeFrom7Years, konsOneYear);
		assertFalse(result.isOK());
	}
	
	@Test
	public void testDignitaet(){
		Konsultation kons = konsGriss;
		setUpDignitaet(kons);
		
		// default mandant type is specialist
		clearKons(kons);
		Result<IVerrechenbar> result = kons.addLeistung(tlBaseFirst5Min);
		assertTrue(result.isOK());
		Verrechnet verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
		assertNotNull(verrechnet);
		int amountAL = TarmedLeistung.getAL(verrechnet);
		assertEquals(1042, amountAL);
		Money amount = verrechnet.getNettoPreis();
		assertEquals(15.45, amount.getAmount(), 0.01);
		
		// set the mandant type to practitioner
		clearKons(kons);
		TarmedLeistung.setMandantType(kons.getMandant(), MandantType.PRACTITIONER);
		result = kons.addLeistung(tlBaseFirst5Min);
		assertTrue(result.isOK());
		verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
		assertNotNull(verrechnet);
		amountAL = TarmedLeistung.getAL(verrechnet);
		assertEquals(969, amountAL);
		amount = verrechnet.getNettoPreis();
		assertEquals(14.84, amount.getAmount(), 0.01);
		
		tearDownDignitaet(kons);
		
		// set the mandant type to practitioner
		clearKons(kons);
		TarmedLeistung.setMandantType(kons.getMandant(), MandantType.SPECIALIST);
		result = kons.addLeistung(tlBaseFirst5Min);
		assertTrue(result.isOK());
		verrechnet = kons.getVerrechnet(tlBaseFirst5Min);
		assertNotNull(verrechnet);
		amountAL = TarmedLeistung.getAL(verrechnet);
		assertEquals(957, amountAL);
		amount = verrechnet.getNettoPreis();
		assertEquals(17.76, amount.getAmount(), 0.01);
	}
	
	private void setUpDignitaet(Konsultation kons){
		Hashtable<String, String> extension = tlBaseFirst5Min.loadExtension();
		// set reduce factor
		extension.put(TarmedLeistung.EXT_FLD_F_AL_R, "0.93");
		// the AL value
		extension.put(TarmedLeistung.EXT_FLD_TP_AL, "10.42");
		// add additional multiplier
		LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
		MultiplikatorList multis =
			new MultiplikatorList("VK_PREISE", kons.getFall().getAbrechnungsSystem());
		multis.insertMultiplikator(new TimeTool(yesterday), "0.83");
		
		tlBaseFirst5Min.setExtension(extension);
	}
	
	private void tearDownDignitaet(Konsultation kons){
		Hashtable<String, String> extension = tlBaseFirst5Min.loadExtension();
		// clear reduce factor
		extension = tlBaseFirst5Min.loadExtension();
		extension.remove(TarmedLeistung.EXT_FLD_F_AL_R);
		// reset AL value
		extension.put(TarmedLeistung.EXT_FLD_TP_AL, "9.57");
		// remove additional multiplier
		LocalDate yesterday = LocalDate.now().minus(1, ChronoUnit.DAYS);
		MultiplikatorList multis =
			new MultiplikatorList("VK_PREISE", kons.getFall().getAbrechnungsSystem());
		multis.removeMultiplikator(new TimeTool(yesterday), "0.83");

		tlBaseFirst5Min.setExtension(extension);
	}
	
	private void clearKons(Konsultation kons){
		for (Verrechnet verrechnet : kons.getLeistungen()) {
			kons.removeLeistung(verrechnet);
		}
	}
	
	private Optional<Verrechnet> getVerrechent(Konsultation kons, TarmedLeistung leistung){
		for (Verrechnet verrechnet : kons.getLeistungen()) {
			if (verrechnet.getCode().equals(leistung.getCode())) {
				return Optional.of(verrechnet);
			}
		}
		return Optional.empty();
	}
}
