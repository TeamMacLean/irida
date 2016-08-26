package ca.corefacility.bioinformatics.irida.repositories.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import ca.corefacility.bioinformatics.irida.model.sample.SampleMetadata;

@Repository
public class SampleMetadataRepositoryImpl implements SampleMetadataRepositoryCustom {
	private MongoTemplate mongoTemplate;

	@Autowired
	public SampleMetadataRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public SampleMetadata save(SampleMetadata metadata) {
		System.out.println("DO IT MYSELF");
		mongoTemplate.save(metadata);
		return metadata;
	}
}
