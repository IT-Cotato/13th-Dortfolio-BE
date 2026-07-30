package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.entity.Record;

public interface RecordEmbeddingWriter {

	void save(Record record, String embeddingModel, float[] embedding);
}
