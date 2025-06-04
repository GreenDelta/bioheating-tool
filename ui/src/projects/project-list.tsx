import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Project } from '../model';
import * as api from '../api';

export const ProjectList = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      const projectList = await api.getProjects();
      setProjects(projectList);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div>Loading projects...</div>;
  }

  if (error) {
    return <div style={{ color: 'red' }}>Error: {error}</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2>My Projects</h2>
        <Link to="/ui/projects/new" style={{
          backgroundColor: '#007bff',
          color: 'white',
          padding: '8px 16px',
          textDecoration: 'none',
          borderRadius: '4px'
        }}>
          Create New Project
        </Link>
      </div>

      {projects.length === 0 ? (
        <div>
          <p>No projects found.</p>
          <Link to="/ui/projects/new">Create your first project</Link>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: '16px' }}>
          {projects.map(project => (
            <div key={project.id} style={{
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '16px',
              backgroundColor: '#f9f9f9'
            }}>
              <h3 style={{ margin: '0 0 8px 0' }}>
                <Link to={`/ui/projects/${project.id}`} style={{ textDecoration: 'none', color: '#007bff' }}>
                  {project.name}
                </Link>
              </h3>
              {project.description && (
                <p style={{ margin: '0', color: '#666' }}>{project.description}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
