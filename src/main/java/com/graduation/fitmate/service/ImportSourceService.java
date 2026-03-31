package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ImportSourceForm;
import com.graduation.fitmate.entity.ImportSource;
import com.graduation.fitmate.mapper.ImportSourceMapper;
import com.graduation.fitmate.mapper.ImportedVideoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportSourceService {

    private final ImportSourceMapper importSourceMapper;
    private final ImportedVideoMapper importedVideoMapper;

    public ImportSourceService(ImportSourceMapper importSourceMapper, ImportedVideoMapper importedVideoMapper) {
        this.importSourceMapper = importSourceMapper;
        this.importedVideoMapper = importedVideoMapper;
    }

    public List<ImportSource> findAll() {
        return importSourceMapper.selectList(new LambdaQueryWrapper<ImportSource>()
                .orderByDesc(ImportSource::getCreatedAt));
    }

    public List<ImportSource> findEnabled() {
        return importSourceMapper.selectList(new LambdaQueryWrapper<ImportSource>()
                .eq(ImportSource::getEnabled, true)
                .orderByAsc(ImportSource::getId));
    }

    public ImportSource findById(Long id) {
        return importSourceMapper.selectById(id);
    }

    @Transactional
    public void save(ImportSourceForm form) {
        ImportSource source = new ImportSource();
        source.setName(form.getName());
        source.setSourceType(form.getSourceType());
        source.setExternalId(form.getExternalId());
        source.setChannelName(form.getChannelName());
        source.setDefaultGoal(form.getDefaultGoal());
        source.setDefaultEquipment(form.getDefaultEquipment());
        source.setDefaultPosture(form.getDefaultPosture());
        source.setAutoApproveConfident(Boolean.TRUE.equals(form.getAutoApproveConfident()));
        source.setEnabled(form.getEnabled() == null || form.getEnabled());
        importSourceMapper.insert(source);
    }

    @Transactional
    public void markImported(Long id) {
        ImportSource source = importSourceMapper.selectById(id);
        if (source != null) {
            importSourceMapper.updateById(source);
        }
    }

    @Transactional
    public void toggleEnabled(Long id) {
        ImportSource source = importSourceMapper.selectById(id);
        if (source == null) {
            return;
        }
        source.setEnabled(!Boolean.TRUE.equals(source.getEnabled()));
        importSourceMapper.updateById(source);
    }

    @Transactional
    public void deleteSource(Long id) {
        Long approvedCount = importedVideoMapper.selectCount(new LambdaQueryWrapper<com.graduation.fitmate.entity.ImportedVideo>()
                .eq(com.graduation.fitmate.entity.ImportedVideo::getSourceId, id)
                .eq(com.graduation.fitmate.entity.ImportedVideo::getImportStatus, "APPROVED"));
        if (approvedCount != null && approvedCount > 0) {
            throw new IllegalStateException("This source already has approved videos. Disable it instead of deleting.");
        }
        importedVideoMapper.delete(new LambdaQueryWrapper<com.graduation.fitmate.entity.ImportedVideo>()
                .eq(com.graduation.fitmate.entity.ImportedVideo::getSourceId, id));
        importSourceMapper.deleteById(id);
    }
}
