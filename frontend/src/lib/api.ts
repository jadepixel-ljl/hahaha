export type UserRole = 'ADMIN' | 'USER';
export type UserStatus = 'ACTIVE' | 'BANNED';

export type User = {
  id: number;
  username: string;
  nickname: string;
  avatarUrl?: string | null;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
};

export type Post = {
  id: number;
  author: User;
  content: string;
  imageUrls: string[];
  likeCount: number;
  commentCount: number;
  likedByMe: boolean;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Comment = {
  id: number;
  postId: number;
  parentId?: number | null;
  author: User;
  content: string;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
  replies: Comment[];
};

export type Captcha = {
  question: string;
  token: string;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type RegisterPayload = LoginPayload & {
  nickname?: string;
  avatarUrl?: string;
  captchaToken: string;
  captchaAnswer: string;
};

export type AuthResponse = {
  token: string;
  user: User;
};

export type CreatePostPayload = {
  content: string;
  imageUrls: string[];
};

export type CreateCommentPayload = {
  content: string;
  parentId?: number | null;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL?.trim() || '/api';
const TOKEN_KEY = 'personal-site-token';

let authToken = localStorage.getItem(TOKEN_KEY) || '';

export function getToken() {
  return authToken;
}

export function setToken(token: string) {
  authToken = token;
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function resolveAssetUrl(url?: string | null) {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  return url.startsWith('/') ? url : `/${url}`;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (authToken) {
    headers.set('Authorization', `Bearer ${authToken}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let message = '请求失败，请稍后再试';
    try {
      const error = await response.json();
      message = error.message || message;
    } catch {
      message = response.statusText || message;
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
  captcha: () => request<Captcha>('/auth/captcha'),
  login: (payload: LoginPayload) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  register: (payload: RegisterPayload) =>
    request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  me: () => request<User>('/auth/me'),
  updateProfile: (payload: { nickname: string; avatarUrl?: string | null }) =>
    request<User>('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  uploadAvatar: (file: File) => upload('/upload/avatar', file),
  uploadImage: (file: File) => upload('/upload/image', file),
  posts: () => request<Post[]>('/posts'),
  createPost: (payload: CreatePostPayload) =>
    request<Post>('/posts', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  deletePost: (id: number) =>
    request<void>(`/posts/${id}`, {
      method: 'DELETE',
    }),
  likePost: (id: number) =>
    request<{ liked: boolean; likeCount: number }>(`/posts/${id}/like`, {
      method: 'POST',
    }),
  unlikePost: (id: number) =>
    request<{ liked: boolean; likeCount: number }>(`/posts/${id}/like`, {
      method: 'DELETE',
    }),
  comments: (postId: number) => request<Comment[]>(`/posts/${postId}/comments`),
  createComment: (postId: number, payload: CreateCommentPayload) =>
    request<Comment>(`/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  deleteComment: (postId: number, id: number) =>
    request<void>(`/posts/${postId}/comments/${id}`, {
      method: 'DELETE',
    }),
  adminUsers: () => request<User[]>('/admin/users'),
  adminUpdateUserStatus: (id: number, status: UserStatus) =>
    request<User>(`/admin/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
  adminDeleteUser: (id: number) =>
    request<void>(`/admin/users/${id}`, {
      method: 'DELETE',
    }),
  adminPosts: () => request<Post[]>('/admin/posts'),
  adminComments: () => request<Comment[]>('/admin/comments'),
  adminDeletePost: (id: number) =>
    request<void>(`/admin/posts/${id}`, {
      method: 'DELETE',
    }),
  adminDeleteComment: (id: number) =>
    request<void>(`/admin/comments/${id}`, {
      method: 'DELETE',
    }),
};

async function upload(path: string, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request<{ url: string }>(path, {
    method: 'POST',
    body: formData,
  });
}
