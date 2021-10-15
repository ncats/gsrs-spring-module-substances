package fda.gsrs.substance.exporters;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class FDASubstanceExporter implements Exporter<Substance> {

	private static final String SOURCE_APP = "app";
	private static final String SOURCE_PROD = "prod";
	private static final String SOURCE_CLINICAL_US = "clinicalus";
	private static final String SOURCE_CLINICAL_EUROPE = "clinicaleurope";
	
	private final boolean showPrivates;
	private String source;

	/*
	SRSProductExporter prodExporter;
	SRSApplicationAllExporter appExporter;
	SRSClinicalTrialUSExporter clinicalUSExporter;
	SRSClinicalTrialEuropeExporter clinicalEuropeExporter;
    */
	public FDASubstanceExporter(OutputStream os, boolean showPrivates, String source) throws IOException {
		this.showPrivates = showPrivates;
		this.source = source;

		/*
		//Export Application Factory
		if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_APP))) {
			OutputFormat format = new OutputFormat("xlsx", "SRS Application Data");
			FDAExporterFactory.Parameters params = new FDAParameters(format, this.showPrivates);
			SRSApplicationAllExporterFactory factory = new SRSApplicationAllExporterFactory();
			this.appExporter = factory.createNewExporter(os, params);	
		} 
        */
		/*
		//Export Product Factory
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_PROD))) {
			OutputFormat format = new OutputFormat("xlsx", "SRS Product Data");
			SRSExporterFactory.Parameters params = new SRSParameters(format, this.showPrivates);
			SRSProductExporterFactory factory = new SRSProductExporterFactory();
			this.prodExporter = factory.createNewExporter(os, params);
		} 
		
		//Export Clinical Trial US Factory
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_CLINICAL_US))) {
			OutputFormat format = new OutputFormat("xlsx", "Clinical Trial US Data");
			SRSExporterFactory.Parameters params = new SRSParameters(format, this.showPrivates);
			SRSClinicalTrialUSExporterFactory factory = new SRSClinicalTrialUSExporterFactory();
			this.clinicalUSExporter = factory.createNewExporter(os, params);
		}
		
		//Export Clinical Trial Europe Factory
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_CLINICAL_EUROPE))) {
			OutputFormat format = new OutputFormat("xlsx", "Clinical Trial Europe Data");
			SRSExporterFactory.Parameters params = new SRSParameters(format, this.showPrivates);
			SRSClinicalTrialEuropeExporterFactory factory = new SRSClinicalTrialEuropeExporterFactory();
			this.clinicalEuropeExporter = factory.createNewExporter(os, params);
		}
		 */
	}

	public String getBdnum(Substance s) {
		return s.codes.stream().filter(cd -> cd.codeSystem.equals("BDNUM")).map(cd -> cd.code).findFirst().orElse(null);
	}

	@Override
	public void export(Substance s) throws IOException {
		/*
		if (!showPrivates && !s.getAccess().isEmpty()) {
			System.out.println("PRIVATE DATA");
			// GSRS-699 skip substances that aren't public unless we have show private data
			// too
			return;
		}
		 */
		String bdnum = getBdnum(s);

		/*
		// Export Application
		if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_APP))) {

			Query<ApplicationAll> query = ApplicationAllFactory.finder.query();
			Expression expr1 = Expr.eq("applicationProductList.applicationIngredientList.bdnum", bdnum);
			List<ApplicationAll> appList = query.where().add(expr1).orderBy("fromTable").findList();

			if (appList.size() > 0) {
				for (ApplicationAll a : appList) {
					appExporter.export(a);
				}
			}
		}

		// Export Product
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_PROD))) {
			Query<ProductAll> query = ProductAllFactory.finder.query();
			Expression expr1 = Expr.eq("substanceId", s.uuid);
			List<ProductAll> list = query.where().add(expr1).findList();

			if (list.size() > 0) {
				for (ProductAll p : list) {
                    prodExporter.export(p);
				}
			}
		}
				
		//Export Clinical Trial US
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_CLINICAL_US))) {
			Query<ClinicalTrialAll> query = ClinicalTrialAllFactory.finder.query();
			Expression expr1 = Expr.ieq("clinicalTrialDrugAll.bdnumName.bdnum", bdnum);
			Expression expr2 = Expr.ieq("fromSource", "US");
			Expression expr = Expr.and(expr1, expr2);
			List<ClinicalTrialAll> list = query.where().add(expr).findList();

			if (list.size() > 0) {
				for (ClinicalTrialAll ct : list) {
					clinicalUSExporter.export(ct);
				}
			}
		}

		//Export Clinical Trial Europe
		else if ((this.source != null) && (this.source.equalsIgnoreCase(SOURCE_CLINICAL_EUROPE))) {
			Query<ClinicalTrialEuropeDetails> query = ClinicalTrialEuropeFactory.finder.query();
			Expression expr1 = Expr.ieq("clinicalTrialEuropeProductList.clinicalTrialEuropeDrugList.bdnumName.bdnum", bdnum);
			List<ClinicalTrialEuropeDetails> list = query.where().add(expr1).findList();

			if (list.size() > 0) {
				for (ClinicalTrialEuropeDetails ct : list) {
					clinicalEuropeExporter.export(ct);
				}
			}
		}
		 */
	}

	@Override
	public void close() throws IOException {
		/*
		if (prodExporter != null) {
			prodExporter.close();
		}
		if (appExporter != null) {
			appExporter.close();
		}
		if (clinicalUSExporter != null) {
			clinicalUSExporter.close();
		}
		if (clinicalEuropeExporter != null) {
			clinicalEuropeExporter.close();
		}*/
	}

	private static class FDAParameters implements FDAExporterFactory.Parameters {
		private final OutputFormat format;

		private final boolean publicOnly;

		FDAParameters(OutputFormat format, boolean publicOnly) {
			Objects.requireNonNull(format);
			this.format = format;
			this.publicOnly = publicOnly;
		}

		@Override
		public OutputFormat getFormat() {
			return format;
		}

		@Override
		public boolean publicOnly() {
			return publicOnly;
		}
	}

}
