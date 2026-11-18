package com.nethub.service;

import com.nethub.entity.Connection;
import com.nethub.repository.ConnectionRepository;
import com.nethub.repository.NetworkInterfaceRepository;
import com.nethub.repository.ProjectRepository;
import com.nethub.exception.ResourceNotFoundException;
import com.nethub.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConnectionService {
    private final ConnectionRepository connectionRepository;
    private final ProjectRepository projectRepository;
    private final NetworkInterfaceRepository interfaceRepository;

    public List<Connection> getConnectionsByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return connectionRepository.findByProjectId(projectId);
    }

    public Connection createConnection(Long projectId, Connection connection) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        
        if (!interfaceRepository.existsById(connection.getSourceInterfaceId())) {
            throw new ResourceNotFoundException("Source interface not found with id: " + connection.getSourceInterfaceId());
        }
        
        if (!interfaceRepository.existsById(connection.getDestinationInterfaceId())) {
            throw new ResourceNotFoundException("Destination interface not found with id: " + connection.getDestinationInterfaceId());
        }
        
        if (Objects.equals(connection.getSourceInterfaceId(), connection.getDestinationInterfaceId())) {
            throw new ValidationException("Cannot connect an interface to itself.");
        }
        
        // Prevent duplicate connections
        List<Connection> existing = connectionRepository.findByProjectId(projectId);
        for (Connection c : existing) {
            if ((c.getSourceInterfaceId().equals(connection.getSourceInterfaceId()) && c.getDestinationInterfaceId().equals(connection.getDestinationInterfaceId())) ||
                (c.getSourceInterfaceId().equals(connection.getDestinationInterfaceId()) && c.getDestinationInterfaceId().equals(connection.getSourceInterfaceId()))) {
                throw new ValidationException("Connection already exists between these interfaces.");
            }
        }
        
        connection.setProjectId(projectId);
        return connectionRepository.save(connection);
    }

    public void deleteConnection(Long id) {
        connectionRepository.deleteById(id);
    }
}
