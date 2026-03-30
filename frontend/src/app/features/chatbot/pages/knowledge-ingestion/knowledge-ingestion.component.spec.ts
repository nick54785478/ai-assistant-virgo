import { ComponentFixture, TestBed } from '@angular/core/testing';

import { KnowledgeIngestionComponent } from './knowledge-ingestion.component';

describe('KnowledgeIngestionComponent', () => {
  let component: KnowledgeIngestionComponent;
  let fixture: ComponentFixture<KnowledgeIngestionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KnowledgeIngestionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(KnowledgeIngestionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
