package ca.corefacility.bioinformatics.irida.service.upload;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;

import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.upload.UploadSample;

/**
 * Used to convert IRIDA Samples to those that can be uploaded into a remote site.
 * 
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 *
 */
public interface UploadSampleConversionService {

	/**
	 * Converts the passed {@link Sample} object to an {@link UploadSample}.
	 *
	 * @param sample
	 *            The sample to convert
	 * @return The corresponding UploadSample.
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#sample, 'canReadSample')")
	public UploadSample convertToUploadSample(Sample sample);

	/**
	 * Converts a set of samples to UploadSamples to be uploaded to Galaxy.
	 *
	 * @param samples
	 *            The samples to upload.
	 * @return A set of UploadSamples.
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#samples, 'canReadSample')")
	public Set<UploadSample> convertToUploadSamples(Set<Sample> samples);
	
	/**
	 * Converts a list of samples to UploadSamples to be uploaded to Galaxy.
	 *
	 * @param samples
	 *            The samples to upload.
	 * @return A set of UploadSamples.
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#samples, 'canReadSample')")
	public Set<UploadSample> convertToUploadSamples(Sample... samples);

	/**
	 * Get the samples for a specific project, identified by its project id.
	 *
	 * @param projectId
	 *            The project ID
	 * @return A set of UploadSamples.
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#projectId, 'canReadProject')")
	public Set<UploadSample> getUploadSamplesForProject(Long projectId);
}