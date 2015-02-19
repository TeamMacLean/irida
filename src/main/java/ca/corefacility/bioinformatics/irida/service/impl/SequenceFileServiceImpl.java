package ca.corefacility.bioinformatics.irida.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.FileProcessorTimeoutException;
import ca.corefacility.bioinformatics.irida.exceptions.InvalidPropertyException;
import ca.corefacility.bioinformatics.irida.exceptions.SampleAnalysisDuplicateException;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.run.SequencingRun;
import ca.corefacility.bioinformatics.irida.model.run.SequencingRun.LayoutType;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.SampleSequenceFileJoin;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFilePair;
import ca.corefacility.bioinformatics.irida.processing.FileProcessingChain;
import ca.corefacility.bioinformatics.irida.repositories.joins.sample.SampleSequenceFileJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.sequencefile.SequenceFilePairRepository;
import ca.corefacility.bioinformatics.irida.repositories.sequencefile.SequenceFileRepository;
import ca.corefacility.bioinformatics.irida.service.SequenceFileService;

/**
 * Implementation for managing {@link SequenceFile}.
 * 
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
@Service
public class SequenceFileServiceImpl extends CRUDServiceImpl<Long, SequenceFile> implements SequenceFileService {

	private static final Logger logger = LoggerFactory.getLogger(SequenceFileServiceImpl.class);

	/**
	 * Reference to {@link SampleSequenceFileJoinRepository}.
	 */
	private final SampleSequenceFileJoinRepository ssfRepository;

	/**
	 * Reference to {@link SequenceFileRepository}.
	 */
	private final SequenceFileRepository sequenceFileRepository;

	private final SequenceFilePairRepository pairRepository;

	/**
	 * Executor for running pipelines asynchronously.
	 */
	private final TaskExecutor fileProcessingChainExecutor;
	/**
	 * File processing chain to execute on sequence files.
	 */
	private final FileProcessingChain fileProcessingChain;

	private static final String FILE_PROPERTY = "file";

	/**
	 * Constructor.
	 * 
	 * @param sequenceFileRepository
	 *            the sequence file repository.
	 * @param validator
	 *            validator.
	 */
	@Autowired
	public SequenceFileServiceImpl(SequenceFileRepository sequenceFileRepository,
			SampleSequenceFileJoinRepository ssfRepository, SequenceFilePairRepository pairRepository,
			@Qualifier("fileProcessingChainExecutor") TaskExecutor executor, FileProcessingChain fileProcessingChain,
			Validator validator) {
		super(sequenceFileRepository, validator, SequenceFile.class);
		this.sequenceFileRepository = sequenceFileRepository;
		this.ssfRepository = ssfRepository;
		this.pairRepository = pairRepository;
		this.fileProcessingChainExecutor = executor;
		this.fileProcessingChain = fileProcessingChain;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public SequenceFile create(SequenceFile sequenceFile) {
		// Send the file to the database repository to be stored (in super)
		logger.trace("Calling super.create");
		SequenceFile sf = super.create(sequenceFile);
		fileProcessingChainExecutor.execute(new SequenceFileProcessorLauncher(fileProcessingChain, sf.getId(),
				SecurityContextHolder.getContext()));
		return sf;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SequenceFile read(Long id) throws EntityNotFoundException {
		return super.read(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public SequenceFile update(Long id, Map<String, Object> updatedFields) throws InvalidPropertyException {
		SequenceFile sf = super.update(id, updatedFields);

		// only launch the file processing chain if the file has been modified.
		if (updatedFields.containsKey(FILE_PROPERTY)) {
			fileProcessingChainExecutor.execute(new SequenceFileProcessorLauncher(fileProcessingChain, sf.getId(),
					SecurityContextHolder.getContext()));
		}

		return sf;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public Join<Sample, SequenceFile> createSequenceFileInSample(SequenceFile sequenceFile, Sample sample) {
		// check for consistency with the sequencing run
		SequencingRun sequencingRun = sequenceFile.getSequencingRun();
		if (sequencingRun != null && sequencingRun.getLayoutType() != LayoutType.SINGLE_END) {
			throw new IllegalArgumentException("Attempting to add single end files to a non single end run.");
		}

		SequenceFile created = create(sequenceFile);
		SampleSequenceFileJoin join = ssfRepository.save(new SampleSequenceFileJoin(sample, created));
		return join;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public List<Join<Sample, SequenceFile>> createSequenceFilePairInSample(SequenceFile file1, SequenceFile file2,
			Sample sample) {
		// check for consistency with the sequencing run
		SequencingRun sequencingRun = file1.getSequencingRun();
		if (sequencingRun != null && sequencingRun.getLayoutType() != LayoutType.PAIRED_END) {
			throw new IllegalArgumentException("Attempting to add paired-end files to a non paired-end run.");
		}

		file1 = create(file1);
		file2 = create(file2);

		List<Join<Sample, SequenceFile>> list = new ArrayList<>();
		list.add(ssfRepository.save(new SampleSequenceFileJoin(sample, file1)));
		list.add(ssfRepository.save(new SampleSequenceFileJoin(sample, file2)));
		pairRepository.save(new SequenceFilePair(file1, file2));

		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Join<Sample, SequenceFile>> getSequenceFilesForSample(Sample sample) {
		return ssfRepository.getFilesForSample(sample);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<SequenceFile> getSequenceFilesForSequencingRun(SequencingRun miseqRun) {
		return sequenceFileRepository.findSequenceFilesForSequencingRun(miseqRun);
	}

	/**
	 * Executes {@link FileProcessingChain} asynchronously in a
	 * {@link TaskExecutor}.
	 * 
	 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
	 * 
	 */
	private static final class SequenceFileProcessorLauncher implements Runnable {
		private final FileProcessingChain fileProcessingChain;
		private final Long sequenceFileId;
		private final SecurityContext securityContext;

		public SequenceFileProcessorLauncher(FileProcessingChain fileProcessingChain, Long sequenceFileId,
				SecurityContext securityContext) {
			this.fileProcessingChain = fileProcessingChain;
			this.sequenceFileId = sequenceFileId;
			this.securityContext = securityContext;
		}

		@Override
		public void run() {
			// when running in single-threaded mode, the security context should
			// already be populated in the current thread and and we shouldn't
			// have to overwrite and erase the context before execution.
			boolean copiedSecurityContext = true;
			SecurityContext context = SecurityContextHolder.getContext();
			if (context == null || context.getAuthentication() == null) {
				SecurityContextHolder.setContext(securityContext);
			} else {
				copiedSecurityContext = false;
			}

			// proceed with analysis
			try {
				fileProcessingChain.launchChain(sequenceFileId);
			} catch (FileProcessorTimeoutException e) {
				logger.error(
						"FileProcessingChain did *not* execute -- the transaction opened by SequenceFileService never closed.",
						e);
			}

			// erase the security context if we copied the context into the
			// current thread.
			if (copiedSecurityContext) {
				SecurityContextHolder.clearContext();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Join<Sample, SequenceFile> getSequenceFileForSample(Sample sample, Long identifier)
			throws EntityNotFoundException {
		Optional<Join<Sample, SequenceFile>> file = getSequenceFilesForSample(sample).stream()
				.filter(j -> j.getObject().getId().equals(identifier)).findFirst();
		if (file.isPresent()) {
			return file.get();
		}

		throw new EntityNotFoundException("Sequence file " + identifier + " does not exist in sample " + sample);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<Sample, SequenceFile> getSequenceFileSingleSamples(Set<SequenceFile> sequenceFiles)
			throws SampleAnalysisDuplicateException {
		Map<Sample, SequenceFile> sampleSequenceFiles = new HashMap<>();

		for (SequenceFile file : sequenceFiles) {
			Join<Sample, SequenceFile> sampleSequenceFile = ssfRepository
					.getSampleForSequenceFile(file);
			Sample sample = sampleSequenceFile.getSubject();
			SequenceFile sequenceFile = sampleSequenceFile.getObject();

			if (sampleSequenceFiles.containsKey(sample)) {
				SequenceFile previousFile = sampleSequenceFiles.get(sample);
				throw new SampleAnalysisDuplicateException("Sequence files " + sequenceFile + ", " + previousFile
						+ " both have the same sample " + sample);
			} else {
				sampleSequenceFiles.put(sample, sequenceFile);
			}
		}

		return sampleSequenceFiles;
	}

	@Override
	public List<Join<Sample, SequenceFile>> getUnpairedSequenceFilesForSample(Sample sample) {
		return ssfRepository.getUnpairedSequenceFilesForSample(sample);
	}
}
