package ca.corefacility.bioinformatics.irida.service.remote.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import ca.corefacility.bioinformatics.irida.model.RemoteAPI;
import ca.corefacility.bioinformatics.irida.model.remote.RemoteProject;
import ca.corefacility.bioinformatics.irida.model.remote.RemoteSample;
import ca.corefacility.bioinformatics.irida.repositories.remote.ProjectRemoteRepository;
import ca.corefacility.bioinformatics.irida.repositories.remote.SampleRemoteRepository;
import ca.corefacility.bioinformatics.irida.service.remote.SampleRemoteService;

@Service
public class SampleRemoteServiceImpl extends RemoteServiceImpl<RemoteSample> implements SampleRemoteService {
	public static final String PROJECT_SAMPLES_REL = "project/samples";
	public static final String SAMPLES_CACHE_NAME = "samplesForProject";

	@Autowired
	public SampleRemoteServiceImpl(SampleRemoteRepository sampleRemoteRepository,
			ProjectRemoteRepository projectRemoteRepository) {
		super(sampleRemoteRepository);
	}

	@Override
	public List<RemoteSample> getSamplesForProject(RemoteProject project, RemoteAPI api) {
		String samplesHref = project.getHrefForRel(PROJECT_SAMPLES_REL);
		return list(samplesHref, api);
	}

	@Override
	public Page<RemoteSample> searchSamplesForProject(RemoteProject project, RemoteAPI api, String search, int page,
			int size) {
		List<RemoteSample> samplesForProject = getSamplesForProject(project, api);
		List<RemoteSample> filtered = samplesForProject.stream()
				.filter(s -> s.getSampleName().toLowerCase().contains(search.toLowerCase()))
				.collect(Collectors.toList());

		int from = Math.max(0, page * size);
		int to = Math.min(filtered.size(), (page + 1) * size);

		List<RemoteSample> paged = filtered.subList(from, to);

		return new PageImpl<>(paged, null, filtered.size());
	}

}
