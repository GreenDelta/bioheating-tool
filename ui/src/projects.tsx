import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Project, ProjectData } from './model';
import * as api from './api';

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

export const ProjectForm = () => {
  const [formData, setFormData] = useState<ProjectData>({
    name: '',
    description: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setError('Project name is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await api.createProject(formData);
      // Redirect to projects list
      window.location.href = '/ui/projects';
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create project');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Create New Project</h2>

      <form onSubmit={handleSubmit} style={{ maxWidth: '500px' }}>
        {error && (
          <div style={{
            color: 'red',
            backgroundColor: '#fee',
            padding: '10px',
            borderRadius: '4px',
            marginBottom: '16px'
          }}>
            {error}
          </div>
        )}

        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="name" style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
            Project Name *
          </label>
          <input
            id="name"
            type="text"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            style={{
              width: '100%',
              padding: '8px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontSize: '14px'
            }}
            required
          />
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label htmlFor="description" style={{ display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
            Description
          </label>
          <textarea
            id="description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            rows={4}
            style={{
              width: '100%',
              padding: '8px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontSize: '14px',
              resize: 'vertical'
            }}
          />
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            type="submit"
            disabled={loading}
            style={{
              backgroundColor: '#007bff',
              color: 'white',
              padding: '10px 20px',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.6 : 1
            }}
          >
            {loading ? 'Creating...' : 'Create Project'}
          </button>

          <Link
            to="/ui/projects"
            style={{
              backgroundColor: '#6c757d',
              color: 'white',
              padding: '10px 20px',
              textDecoration: 'none',
              borderRadius: '4px',
              display: 'inline-block'
            }}
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
};

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
