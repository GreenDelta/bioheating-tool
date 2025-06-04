import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Project } from '../model';
import * as api from '../api';

export const ProjectDetail = ({ projectId }: { projectId: number }) => {
  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProject();
  }, [projectId]);

  const loadProject = async () => {
    try {
      setLoading(true);
      const projectData = await api.getProject(projectId);
      setProject(projectData);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load project');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div>Loading project...</div>;
  }

  if (error) {
    return <div style={{ color: 'red' }}>Error: {error}</div>;
  }

  if (!project) {
    return <div>Project not found</div>;
  }

  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <Link to="/ui/projects" style={{ color: '#007bff', textDecoration: 'none' }}>
          ← Back to Projects
        </Link>
      </div>

      <h2>{project.name}</h2>

      {project.description && (
        <div style={{
          backgroundColor: '#f8f9fa',
          padding: '16px',
          borderRadius: '8px',
          marginTop: '16px'
        }}>
          <h3>Description</h3>
          <p style={{ margin: 0 }}>{project.description}</p>
        </div>
      )}

      <div style={{ marginTop: '32px' }}>
        <h3>Project Details</h3>
        <p><strong>ID:</strong> {project.id}</p>
        <p><strong>Name:</strong> {project.name}</p>
      </div>
    </div>
  );
};
