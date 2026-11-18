package com.nethub.service;

import com.nethub.entity.Project;
import com.nethub.repository.ProjectRepository;
import com.nethub.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    public Project updateProject(Long id, Project updatedProject) {
        Project existing = getProject(id);
        existing.setName(updatedProject.getName());
        existing.setDescription(updatedProject.getDescription());
        return projectRepository.save(existing);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
