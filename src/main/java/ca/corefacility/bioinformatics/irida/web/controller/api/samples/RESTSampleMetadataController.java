package ca.corefacility.bioinformatics.irida.web.controller.api.samples;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.corefacility.bioinformatics.irida.model.IridaResourceSupport;
import ca.corefacility.bioinformatics.irida.model.sample.MetadataTemplateField;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.web.controller.api.RESTGenericController;
import ca.corefacility.bioinformatics.irida.web.controller.api.projects.RESTProjectSamplesController;

/**
 * REST controller to handle storing and retrieving metadata from a
 * {@link Sample}
 */
@Controller
public class RESTSampleMetadataController {
	private static final Logger logger = LoggerFactory.getLogger(RESTSampleMetadataController.class);

	public static final String METADATA_REL = "sample/metadata";
	public static final String SAMPLE_REL = "sample";

	private SampleService sampleService;
	private MetadataTemplateService metadataTemplateService;

	@Autowired
	public RESTSampleMetadataController(SampleService sampleService, MetadataTemplateService metadataTemplateService) {
		this.sampleService = sampleService;
		this.metadataTemplateService = metadataTemplateService;
	}

	/**
	 * Get the metadata for a given {@link Sample}
	 * 
	 * @param sampleId
	 *            the id of the {@link Sample} to get metadata for
	 * @return the metadata for the sample
	 */
	@RequestMapping(value = "/api/samples/{sampleId}/metadata", method = RequestMethod.GET)
	public ModelMap getSampleMetadata(@PathVariable Long sampleId) {
		logger.trace("Getting sample metadata for " + sampleId);
		ModelMap modelMap = new ModelMap();
		Sample s = sampleService.read(sampleId);

		SampleMetadataResponse response = buildSampleMetadataResponse(s);

		modelMap.addAttribute(RESTGenericController.RESOURCE_NAME, response);
		return modelMap;
	}

	/**
	 * Save new metadata for a {@link Sample}. Note this will overwrite the
	 * existing metadata
	 * 
	 * @param sampleId
	 *            the id of the {@link Sample} to save new metadata
	 * @param metadataMap
	 *            the metadata to save to the {@link Sample}
	 * @return the updated {@link Sample}
	 */
	@RequestMapping(value = "/api/samples/{sampleId}/metadata", method = RequestMethod.POST)
	public ModelMap saveSampleMetadata(@PathVariable Long sampleId,
			@RequestBody Map<String, MetadataEntry> metadataMap) {
		Sample s = sampleService.read(sampleId);

		Map<MetadataTemplateField, MetadataEntry> metadata = metadataTemplateService.getMetadataMap(metadataMap);

		s.setMetadata(metadata);

		sampleService.update(s);

		return getSampleMetadata(sampleId);
	}

	/**
	 * Add select new metadata fields to the {@link Sample}. Note this will only
	 * overwrite duplicate terms. Existing metadata will not be affected.
	 * 
	 * @param sampleId
	 *            the {@link Sample} to add metadata to
	 * @param metadataMap
	 *            the new metadata
	 * @return the updated {@link Sample}
	 */
	@RequestMapping(value = "/api/samples/{sampleId}/metadata", method = RequestMethod.PUT)
	public ModelMap addSampleMetadata(@PathVariable Long sampleId,
			@RequestBody Map<String, MetadataEntry> metadataMap) {
		Sample s = sampleService.read(sampleId);

		Map<MetadataTemplateField, MetadataEntry> metadata = metadataTemplateService.getMetadataMap(metadataMap);

		s.mergeMetadata(metadata);

		sampleService.update(s);

		return getSampleMetadata(sampleId);
	}

	/**
	 * Build a {@link SampleMetadataResponse} object
	 * 
	 * @param s
	 *            the {@link Sample} to build the object from
	 * @return a constructed {@link SampleMetadataResponse}
	 */
	private SampleMetadataResponse buildSampleMetadataResponse(final Sample s) {
		SampleMetadataResponse response = new SampleMetadataResponse(s.getMetadata());
		response.add(linkTo(methodOn(RESTSampleMetadataController.class).getSampleMetadata(s.getId())).withSelfRel());
		response.add(linkTo(methodOn(RESTProjectSamplesController.class).getSample(s.getId())).withRel(SAMPLE_REL));
		return response;
	}

	/**
	 * Response class so we can add links to sample metadata
	 */
	private class SampleMetadataResponse extends IridaResourceSupport {
		Map<MetadataTemplateField, MetadataEntry> metadata;

		public SampleMetadataResponse(Map<MetadataTemplateField, MetadataEntry> metadata) {
			this.metadata = metadata;
		}

		@JsonProperty
		public Map<MetadataTemplateField, MetadataEntry> getMetadata() {
			return metadata;
		}

		@JsonProperty
		public void setMetadata(Map<MetadataTemplateField, MetadataEntry> metadata) {
			this.metadata = metadata;
		}
	}
}
