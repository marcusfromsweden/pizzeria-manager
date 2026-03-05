import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './Button';
import { Input } from './Input';
import { Alert } from './Alert';
import { Card, CardHeader, CardTitle, CardContent } from './Card';
import { Badge } from './Badge';
import { Select } from './Select';
import { Spinner } from './Spinner';
import { createRef } from 'react';

describe('Button', () => {
  it('should render with children', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument();
  });

  it('should render with primary variant by default', () => {
    render(<Button>Primary</Button>);
    const button = screen.getByRole('button');
    expect(button.className).toContain('bg-primary-600');
  });

  it('should render different variants', () => {
    const { rerender } = render(<Button variant="secondary">Button</Button>);
    expect(screen.getByRole('button').className).toContain('bg-slate-200');

    rerender(<Button variant="danger">Button</Button>);
    expect(screen.getByRole('button').className).toContain('bg-red-600');

    rerender(<Button variant="ghost">Button</Button>);
    expect(screen.getByRole('button').className).toContain('bg-transparent');
  });

  it('should render different sizes', () => {
    const { rerender } = render(<Button size="sm">Button</Button>);
    expect(screen.getByRole('button').className).toContain('text-xs');

    rerender(<Button size="md">Button</Button>);
    expect(screen.getByRole('button').className).toContain('text-sm');

    rerender(<Button size="lg">Button</Button>);
    expect(screen.getByRole('button').className).toContain('text-base');
  });

  it('should show spinner when isLoading', () => {
    render(<Button isLoading>Loading</Button>);
    expect(document.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('should be disabled when isLoading', () => {
    render(<Button isLoading>Loading</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('should be disabled when disabled prop is true', () => {
    render(<Button disabled>Disabled</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('should call onClick when clicked', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();

    render(<Button onClick={handleClick}>Click me</Button>);
    await user.click(screen.getByRole('button'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('should forward ref', () => {
    const ref = createRef<HTMLButtonElement>();
    render(<Button ref={ref}>Button</Button>);
    expect(ref.current).toBeInstanceOf(HTMLButtonElement);
  });

  it('should have displayName', () => {
    expect(Button.displayName).toBe('Button');
  });
});

describe('Input', () => {
  it('should render without label', () => {
    render(<Input placeholder="Enter text" />);
    expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
  });

  it('should render with label', () => {
    render(<Input label="Email" />);
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('should generate id from label', () => {
    render(<Input label="User Name" />);
    const input = screen.getByLabelText('User Name');
    expect(input.id).toBe('user-name');
  });

  it('should use provided id over generated id', () => {
    render(<Input label="Email" id="custom-id" />);
    const input = screen.getByLabelText('Email');
    expect(input.id).toBe('custom-id');
  });

  it('should show error message', () => {
    render(<Input label="Email" error="Invalid email" />);
    expect(screen.getByText('Invalid email')).toBeInTheDocument();
  });

  it('should have error styles when error is present', () => {
    render(<Input label="Email" error="Invalid email" />);
    const input = screen.getByLabelText('Email');
    expect(input.className).toContain('border-red-500');
  });

  it('should set aria-invalid when error is present', () => {
    render(<Input label="Email" error="Invalid email" />);
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
  });

  it('should show helper text when no error', () => {
    render(<Input label="Email" helperText="We'll never share your email" />);
    expect(screen.getByText("We'll never share your email")).toBeInTheDocument();
  });

  it('should not show helper text when error is present', () => {
    render(
      <Input label="Email" error="Invalid" helperText="We'll never share your email" />
    );
    expect(screen.queryByText("We'll never share your email")).not.toBeInTheDocument();
  });

  it('should forward ref', () => {
    const ref = createRef<HTMLInputElement>();
    render(<Input ref={ref} />);
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });

  it('should have displayName', () => {
    expect(Input.displayName).toBe('Input');
  });
});

describe('Alert', () => {
  it('should render children', () => {
    render(<Alert>Alert message</Alert>);
    expect(screen.getByText('Alert message')).toBeInTheDocument();
  });

  it('should render with title', () => {
    render(<Alert title="Alert Title">Message</Alert>);
    expect(screen.getByText('Alert Title')).toBeInTheDocument();
  });

  it('should have role="alert"', () => {
    render(<Alert>Message</Alert>);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('should render different variants', () => {
    const { rerender } = render(<Alert variant="success">Message</Alert>);
    expect(screen.getByRole('alert').className).toContain('bg-green-50');

    rerender(<Alert variant="error">Message</Alert>);
    expect(screen.getByRole('alert').className).toContain('bg-red-50');

    rerender(<Alert variant="warning">Message</Alert>);
    expect(screen.getByRole('alert').className).toContain('bg-yellow-50');

    rerender(<Alert variant="info">Message</Alert>);
    expect(screen.getByRole('alert').className).toContain('bg-blue-50');
  });

  it('should show close button when onClose is provided', () => {
    const handleClose = vi.fn();
    render(<Alert onClose={handleClose}>Message</Alert>);
    expect(screen.getByRole('button', { name: /dismiss/i })).toBeInTheDocument();
  });

  it('should not show close button when onClose is not provided', () => {
    render(<Alert>Message</Alert>);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('should call onClose when close button is clicked', async () => {
    const handleClose = vi.fn();
    const user = userEvent.setup();

    render(<Alert onClose={handleClose}>Message</Alert>);
    await user.click(screen.getByRole('button', { name: /dismiss/i }));

    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});

describe('Card', () => {
  it('should render children', () => {
    render(<Card>Card content</Card>);
    expect(screen.getByText('Card content')).toBeInTheDocument();
  });

  it('should apply padding based on padding prop', () => {
    const { rerender, container } = render(<Card padding="none">Content</Card>);
    expect(container.firstChild).not.toHaveClass('p-3', 'p-4', 'p-6');

    rerender(<Card padding="sm">Content</Card>);
    expect(container.firstChild).toHaveClass('p-3');

    rerender(<Card padding="md">Content</Card>);
    expect(container.firstChild).toHaveClass('p-4');

    rerender(<Card padding="lg">Content</Card>);
    expect(container.firstChild).toHaveClass('p-6');
  });

  it('should apply custom className', () => {
    const { container } = render(<Card className="custom-class">Content</Card>);
    expect(container.firstChild).toHaveClass('custom-class');
  });
});

describe('CardHeader', () => {
  it('should render children', () => {
    render(<CardHeader>Header</CardHeader>);
    expect(screen.getByText('Header')).toBeInTheDocument();
  });

  it('should have border-bottom styling', () => {
    const { container } = render(<CardHeader>Header</CardHeader>);
    expect(container.firstChild).toHaveClass('border-b');
  });
});

describe('CardTitle', () => {
  it('should render as h3', () => {
    render(<CardTitle>Title</CardTitle>);
    expect(screen.getByRole('heading', { level: 3 })).toHaveTextContent('Title');
  });
});

describe('CardContent', () => {
  it('should render children', () => {
    render(<CardContent>Content</CardContent>);
    expect(screen.getByText('Content')).toBeInTheDocument();
  });
});

describe('Badge', () => {
  it('should render children', () => {
    render(<Badge>Label</Badge>);
    expect(screen.getByText('Label')).toBeInTheDocument();
  });

  it('should render default variant by default', () => {
    render(<Badge>Label</Badge>);
    expect(screen.getByText('Label').className).toContain('bg-slate-100');
  });

  it('should render different variants', () => {
    const { rerender } = render(<Badge variant="success">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('bg-green-100');

    rerender(<Badge variant="warning">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('bg-yellow-100');

    rerender(<Badge variant="danger">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('bg-red-100');

    rerender(<Badge variant="info">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('bg-blue-100');
  });

  it('should render different sizes', () => {
    const { rerender } = render(<Badge size="sm">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('text-xs');

    rerender(<Badge size="md">Label</Badge>);
    expect(screen.getByText('Label').className).toContain('text-sm');
  });
});

describe('Select', () => {
  const options = [
    { value: 'a', label: 'Option A' },
    { value: 'b', label: 'Option B' },
  ];

  it('should render options', () => {
    render(<Select options={options} />);
    expect(screen.getByRole('option', { name: 'Option A' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Option B' })).toBeInTheDocument();
  });

  it('should render with label', () => {
    render(<Select label="Choose" options={options} />);
    expect(screen.getByLabelText('Choose')).toBeInTheDocument();
  });

  it('should render placeholder when provided', () => {
    render(<Select options={options} placeholder="Select one" />);
    expect(screen.getByRole('option', { name: 'Select one' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Select one' })).toBeDisabled();
  });

  it('should show error message', () => {
    render(<Select options={options} error="Required" />);
    expect(screen.getByText('Required')).toBeInTheDocument();
  });

  it('should have error styles when error is present', () => {
    render(<Select label="Test" options={options} error="Required" />);
    expect(screen.getByLabelText('Test').className).toContain('border-red-500');
  });

  it('should set aria-invalid when error is present', () => {
    render(<Select label="Test" options={options} error="Required" />);
    expect(screen.getByLabelText('Test')).toHaveAttribute('aria-invalid', 'true');
  });

  it('should forward ref', () => {
    const ref = createRef<HTMLSelectElement>();
    render(<Select ref={ref} options={options} />);
    expect(ref.current).toBeInstanceOf(HTMLSelectElement);
  });

  it('should have displayName', () => {
    expect(Select.displayName).toBe('Select');
  });
});

describe('Spinner', () => {
  it('should render with role="status"', () => {
    render(<Spinner />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should have aria-label', () => {
    render(<Spinner />);
    expect(screen.getByRole('status')).toHaveAttribute('aria-label');
  });

  it('should have sr-only text for screen readers', () => {
    render(<Spinner />);
    expect(screen.getByRole('status').querySelector('.sr-only')).toBeInTheDocument();
  });

  it('should render different sizes', () => {
    const { rerender } = render(<Spinner size="sm" />);
    expect(screen.getByRole('status').className).toContain('h-4');

    rerender(<Spinner size="md" />);
    expect(screen.getByRole('status').className).toContain('h-8');

    rerender(<Spinner size="lg" />);
    expect(screen.getByRole('status').className).toContain('h-12');
  });

  it('should have animate-spin class', () => {
    render(<Spinner />);
    expect(screen.getByRole('status')).toHaveClass('animate-spin');
  });
});
