package org.ohdsi.webapi.util;

import com.opencsv.CSVWriter;
import org.ohdsi.circe.vocabulary.Concept;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.commondto.SecuredEntityDTO;
import org.ohdsi.webapi.commondto.TaggableEntityDTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportUtil {
    public static ByteArrayOutputStream writeConceptSetExportToCSVAndZip(List<ConceptSetExport> conceptSetExportList)
            throws RuntimeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        StringWriter sw;
        CSVWriter csvWriter;
        String[] headings;
        try {
            // Write Concept Set Expression to a CSV
            sw = new StringWriter();
            csvWriter = new CSVWriter(sw);
            ArrayList<String[]> allLines = new ArrayList<String[]>();
            headings = "Concept Set ID#Name#Concept ID#Concept Code#Concept Name#Domain#Vocabulary#Standard Concept#Exclude#Descendants#Mapped".split(
                    "#");
            allLines.add(headings);
            for (ConceptSetExport cs : conceptSetExportList) {
                for (ConceptSetExpression.ConceptSetItem item : cs.csExpression.items) {
                    ArrayList<String> csItem = new ArrayList<String>();
                    csItem.add(String.valueOf(cs.ConceptSetId));
                    csItem.add(cs.ConceptSetName);
                    csItem.add(String.valueOf(item.concept.conceptId));
                    csItem.add(String.valueOf(item.concept.conceptCode));
                    csItem.add(item.concept.conceptName);
                    csItem.add(item.concept.domainId);
                    csItem.add(item.concept.vocabularyId);
                    csItem.add(item.concept.standardConcept);
                    csItem.add(String.valueOf(item.isExcluded));
                    csItem.add(String.valueOf(item.includeDescendants));
                    csItem.add(String.valueOf(item.includeMapped));
                    allLines.add(csItem.toArray(new String[0]));
                }
            }
            csvWriter.writeAll(allLines);
            csvWriter.close();

            ZipEntry resultsEntry = new ZipEntry("conceptSetExpression.csv");
            zos.putNextEntry(resultsEntry);
            zos.write(sw.getBuffer().toString().getBytes());

            // Write included concepts to a CSV
            sw = new StringWriter();
            csvWriter = new CSVWriter(sw);
            allLines = new ArrayList<>();
            headings = "Concept Set ID#Name#Concept ID#Concept Code#Concept Name#Concept Class ID#Domain#Vocabulary".split(
                    "#");
            allLines.add(headings);
            for (ConceptSetExport cs : conceptSetExportList) {
                for (Concept c : cs.identifierConcepts) {
                    ArrayList<String> csItem = new ArrayList<>();
                    csItem.add(String.valueOf(cs.ConceptSetId));
                    csItem.add(cs.ConceptSetName);
                    csItem.add(String.valueOf(c.conceptId));
                    csItem.add(String.valueOf(c.conceptCode));
                    csItem.add(c.conceptName);
                    csItem.add(c.conceptClassId);
                    csItem.add(c.domainId);
                    csItem.add(c.vocabularyId);
                    allLines.add(csItem.toArray(new String[0]));
                }
            }
            csvWriter.writeAll(allLines);
            csvWriter.close();

            resultsEntry = new ZipEntry("includedConcepts.csv");
            zos.putNextEntry(resultsEntry);
            zos.write(sw.getBuffer().toString().getBytes());

            // Write mapped concepts to a CSV
            sw = new StringWriter();
            csvWriter = new CSVWriter(sw);
            allLines = new ArrayList<>();
            headings = "Concept Set ID#Name#Concept ID#Concept Code#Concept Name#Concept Class ID#Domain#Vocabulary".split(
                    "#");
            allLines.add(headings);
            for (ConceptSetExport cs : conceptSetExportList) {
                for (Concept c : cs.mappedConcepts) {
                    ArrayList<String> csItem = new ArrayList<>();
                    csItem.add(String.valueOf(cs.ConceptSetId));
                    csItem.add(cs.ConceptSetName);
                    csItem.add(String.valueOf(c.conceptId));
                    csItem.add(String.valueOf(c.conceptCode));
                    csItem.add(c.conceptName);
                    csItem.add(c.conceptClassId);
                    csItem.add(c.domainId);
                    csItem.add(c.vocabularyId);
                    allLines.add(csItem.toArray(new String[0]));
                }
            }
            csvWriter.writeAll(allLines);
            csvWriter.close();

            resultsEntry = new ZipEntry("mappedConcepts.csv");
            zos.putNextEntry(resultsEntry);
            zos.write(sw.getBuffer().toString().getBytes());

            zos.closeEntry();
            zos.close();
            baos.flush();
            baos.close();
            return baos;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void clearCreateAndUpdateInfo(SecuredEntityDTO securedEntityDTO) {
        clearMetadataInfo(securedEntityDTO);
    }

    public static void clearCreateAndUpdateInfo(TaggableEntityDTO commonEntityDTO) {
        // prevent tags from being exported
        commonEntityDTO.setTags(null);
        clearMetadataInfo(commonEntityDTO);
    }

    private static void clearMetadataInfo(SecuredEntityDTO securedEntityDTO) {
        securedEntityDTO.setCreatedBy(null);
        securedEntityDTO.setCreatedDate(null);
        securedEntityDTO.setModifiedBy(null);
        securedEntityDTO.setModifiedDate(null);
    }
}
