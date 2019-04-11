package ch.elexis.data;

import java.io.FileInputStream;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;

import ch.elexis.core.interfaces.IReferenceDataImporter;
import ch.elexis.core.services.IReferenceDataImporterService;
import ch.elexis.core.ui.util.CoreUiUtil;
import ch.elexis.core.ui.util.ImporterPage;

public class ComplementaryImporter extends ImporterPage {
	
	@Inject
	private IReferenceDataImporterService importerService;
	
	public ComplementaryImporter(){
		CoreUiUtil.injectServicesWithContext(this);
	}
	
	@Override
	public Composite createPage(Composite parent){
		return new FileBasedImporter(parent, this);
	}
	
	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		
		IReferenceDataImporter importer = importerService.getImporter("complementary")
			.orElseThrow(() -> new IllegalStateException("No IReferenceDataImporter available."));
		return importer.performImport(monitor, new FileInputStream(results[0]), null);
	}
	
	@Override
	public String getDescription(){
		return "Komplementärmedizin-Tarif";
	}
	
	@Override
	public String getTitle(){
		return "Komplementärmedizin";
	}
}
